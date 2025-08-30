package cqrses.service;

import com.eventstore.dbclient.EventStoreDBClient;
import com.eventstore.dbclient.ReadStreamOptions;
import cqrses.command.*;
import cqrses.dto.StreamEventDTO;
import cqrses.entity.EventFilter;
import cqrses.projection.UserInduState;
import cqrses.projection.UserInduStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class InduErrorService {

    private final CommandGateway commandGateway;
    private final UserInduStateRepository userInduStateRepository;
    private final EventStoreDBClient eventStoreDBClient;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    // ----------------- USER INIT -----------------
    public void initializeUser(String userId) {
        validateUserId(userId);
        commandGateway.sendAndWait(new CreateInduErrorCommand(userId));
        log.info("✅ User initialized: {}", userId);
    }

    // ----------------- ADD INDU ERROR -----------------
    public void addInduError(String userId, Double amount) {
        validateUserId(userId);
        validateAmount(amount);
        commandGateway.sendAndWait(new AddInduErrorCommand(userId, amount));
        log.info("➕ InduError added: {} | amount={}", userId, amount);
    }

    // ----------------- ADD COMPENSATION -----------------
    public void addCompensation(String userId, Double amount) {
        validateUserId(userId);
        validateAmount(amount);
        commandGateway.sendAndWait(new AddCompensationCommand(userId, amount));
        log.info("➕ Compensation added: {} | amount={}", userId, amount);
    }

    // ----------------- PROCESS ERRORS -----------------
    // ----------------- PROCESS ERRORS -----------------
    public double processErrors(String userId) {
        validateUserId(userId);

        UserInduState state = userInduStateRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // Handle all NOT_TRAITED InduErrors
        state.getInduErrors().stream()
                .filter(e -> "NOT_TRAITED".equals(e.getStatus()))
                .forEach(error -> {
                    commandGateway.sendAndWait(new HandleInduErrorCommand(userId, error.getInduErrorId(), error.getAmount()));
                    error.setStatus("TRAITED");
                    error.setHandledAt(Instant.now());
                    log.info("🔁 InduError handled: {}", error.getInduErrorId());
                });

        // Handle all NOT_TRAITED Compensations
        state.getCompensations().stream()
                .filter(c -> "NOT_TRAITED".equals(c.getStatus()))
                .forEach(comp -> {
                    commandGateway.sendAndWait(new HandleCompensationCommand(userId, comp.getCompensationId(), comp.getAmount()));
                    comp.setStatus("TRAITED");
                    comp.setHandledAt(Instant.now());
                    log.info("🔁 Compensation handled: {}", comp.getCompensationId());
                });

        // Recalculate all totals including netTotal
        state.recalcTotals();

        // Save updated projection
        userInduStateRepository.save(state);

        log.info("✅ Totals after processing: HandledIndu={} | HandledComp={} | Net={}",
                state.getTotalHandledInduErrors(),
                state.getTotalHandledCompensations(),
                state.getNetTotal());

        // Send final total command
        commandGateway.sendAndWait(new ProcessInduErrorsCommand(userId, state.getNetTotal()));

        return state.getNetTotal();
    }





    // ----------------- GET EVENTS FROM PROJECTIONS -----------------
    public List<StreamEventDTO> getProjectionEvents(
            String userId,
            EventFilter filter,
            int page,
            int size,
            String eventType,
            String fromDate,
            String toDate
    ) {
        validateUserId(userId);

        UserInduState state = userInduStateRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        List<StreamEventDTO> events = new ArrayList<>();

        // Parse optional date filters safely
        Instant fromInstant = (fromDate != null && !fromDate.isBlank()) ? Instant.parse(fromDate) : null;
        Instant toInstant = (toDate != null && !toDate.isBlank()) ? Instant.parse(toDate) : null;

        // --- INDUE ERRORS ---
        for (UserInduState.InduErrorInfo error : state.getInduErrors()) {
            if (eventType != null && !eventType.isBlank() &&
                    !eventType.equals("InduErrorCreatedEvent") &&
                    !eventType.equals("InduErrorHandledEvent")) continue;

            boolean include = switch (filter) {
                case ALL -> true;
                case ONLY_HANDLED -> "TRAITED".equals(error.getStatus());
                case ONLY_UNHANDLED -> "NOT_TRAITED".equals(error.getStatus());
            };

            Instant createdAt = error.getCreatedAt();
            Instant handledAt = error.getHandledAt();

            // Skip events outside date range
            if (createdAt != null) {
                if ((fromInstant != null && createdAt.isBefore(fromInstant)) ||
                        (toInstant != null && createdAt.isAfter(toInstant))) {
                    continue;
                }
            }

            if (include) {
                if (createdAt != null) {
                    events.add(new StreamEventDTO("InduErrorCreatedEvent", error, FORMATTER.format(createdAt)));
                } else {
                    events.add(new StreamEventDTO("InduErrorCreatedEvent", error, "N/A"));
                }

                if ("TRAITED".equals(error.getStatus())) {
                    if (handledAt != null) {
                        events.add(new StreamEventDTO("InduErrorHandledEvent", error, FORMATTER.format(handledAt)));
                    } else {
                        events.add(new StreamEventDTO("InduErrorHandledEvent", error, "N/A"));
                    }
                }
            }
        }

        // --- COMPENSATIONS ---
        for (UserInduState.CompensationInfo comp : state.getCompensations()) {
            if (eventType != null && !eventType.isBlank() &&
                    !eventType.equals("CompensationCreatedEvent") &&
                    !eventType.equals("CompensationHandledEvent")) continue;

            boolean include = switch (filter) {
                case ALL -> true;
                case ONLY_HANDLED -> "TRAITED".equals(comp.getStatus());
                case ONLY_UNHANDLED -> "NOT_TRAITED".equals(comp.getStatus());
            };

            Instant createdAt = comp.getCreatedAt();
            Instant handledAt = comp.getHandledAt();

            if (createdAt != null) {
                if ((fromInstant != null && createdAt.isBefore(fromInstant)) ||
                        (toInstant != null && createdAt.isAfter(toInstant))) {
                    continue;
                }
            }

            if (include) {
                if (createdAt != null) {
                    events.add(new StreamEventDTO("CompensationCreatedEvent", comp, FORMATTER.format(createdAt)));
                } else {
                    events.add(new StreamEventDTO("CompensationCreatedEvent", comp, "N/A"));
                }

                if ("TRAITED".equals(comp.getStatus())) {
                    if (handledAt != null) {
                        events.add(new StreamEventDTO("CompensationHandledEvent", comp, FORMATTER.format(handledAt)));
                    } else {
                        events.add(new StreamEventDTO("CompensationHandledEvent", comp, "N/A"));
                    }
                }
            }
        }

        // --- Sort newest first + paginate ---
        events.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, events.size());
        if (fromIndex >= events.size()) return Collections.emptyList();

        return events.subList(fromIndex, toIndex);
    }

    private void validateUserId(String userId) {
        if (userId == null || userId.isBlank())
            throw new IllegalArgumentException("User ID must not be null or empty.");
    }

    private void validateAmount(Double amount) {
        if (amount == null || amount <= 0)
            throw new IllegalArgumentException("Amount must be greater than 0.");
    }

    public boolean streamExistsForUser(String userId) {
        String streamId = "EventAggregate-" + userId;
        try {
            eventStoreDBClient.readStream(streamId, ReadStreamOptions.get().forwards().fromStart()).get();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    public UserInduState getUserState(String userId) {
        return userInduStateRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

}
