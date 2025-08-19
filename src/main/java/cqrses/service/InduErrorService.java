package cqrses.service;

import com.eventstore.dbclient.*;
import cqrses.command.*;
import cqrses.dto.StreamEventDTO;
import cqrses.entity.CompensationStatus;
import cqrses.entity.InduErrorStatus;
import cqrses.entity.EventFilter;
import cqrses.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.serialization.SerializedObject;
import org.axonframework.serialization.Serializer;
import org.axonframework.serialization.SimpleSerializedObject;
import org.axonframework.serialization.SimpleSerializedType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
@Slf4j
public class InduErrorService {

    private final CommandGateway commandGateway;
    private final EventStoreDBClient eventStoreDBClient;
    private final Serializer serializer;

    public void initializeUser(String userId) {
        validateUserId(userId);
        commandGateway.sendAndWait(new CreateInduErrorCommand(userId));
        log.info("✅ User initialized: {}", userId);
    }

    public void addInduError(String userId, Double amount) {
        validateUserId(userId);
        validateAmount(amount);
        commandGateway.sendAndWait(new AddInduErrorCommand(userId, amount));
        log.info("➕ InduError added: {} | amount={}", userId, amount);
    }

    public void addCompensation(String userId, Double amount) {
        validateUserId(userId);
        validateAmount(amount);
        commandGateway.sendAndWait(new AddCompensationCommand(userId, amount));
        log.info("➕ Compensation added: {} | amount={}", userId, amount);
    }

    @Transactional
    public double processErrors(String userId) throws ExecutionException, InterruptedException {
        validateUserId(userId);

        String streamId = "EventAggregate-" + userId;
        double induTotal = 0.0;
        double compensationTotal = 0.0;

        ReadResult result = eventStoreDBClient.readStream(streamId, ReadStreamOptions.get().forwards().fromStart()).get();

        for (ResolvedEvent resolved : result.getEvents()) {
            RecordedEvent event = resolved.getEvent();
            String type = event.getEventType();

            switch (type) {
                case "InduErrorCreatedEvent" -> {
                    InduErrorCreatedEvent indu = deserializeEvent(event, InduErrorCreatedEvent.class);
                    if (indu.getStatus() == InduErrorStatus.NOT_TRAITED) {
                        induTotal += indu.getAmount();
                        commandGateway.sendAndWait(new HandleInduErrorCommand(userId, indu.getInduErrorId(), indu.getAmount()));
                        log.info("🔁 InduError handled: {}", indu.getInduErrorId());
                    }
                }

                case "CompensationCreatedEvent" -> {
                    CompensationCreatedEvent comp = deserializeEvent(event, CompensationCreatedEvent.class);
                    if (comp.getStatus() == CompensationStatus.NOT_TRAITED) {
                        compensationTotal += comp.getAmount();
                        commandGateway.sendAndWait(new HandleCompensationCommand(userId, comp.getCompensationId(), comp.getAmount()));
                        log.info("🔁 Compensation handled: {}", comp.getCompensationId());
                    }
                }

                default -> log.debug("🔎 Ignored event type: {}", type);
            }
        }

        double netTotal = induTotal - compensationTotal;
        log.info("✅ Total Indu: {} | Total Compensation: {} | ➡ Net: {}", induTotal, compensationTotal, netTotal);

        commandGateway.sendAndWait(new ProcessInduErrorsCommand(userId, netTotal));
        return netTotal;
    }

    public List<StreamEventDTO> getStreamEvents(
            String userId,
            EventFilter filter,
            int page,
            int size,
            String eventType,   // optional event type
            String fromDate,    // optional start date in ISO string
            String toDate       // optional end date in ISO string
    ) throws ExecutionException, InterruptedException {

        validateUserId(userId);

        if (page < 0) page = 0;
        if (size <= 0) size = 10;

        String streamId = "EventAggregate-" + userId;

        ReadResult result = eventStoreDBClient
                .readStream(streamId, ReadStreamOptions.get().forwards().fromStart())
                .get();

        List<ResolvedEvent> resolvedEvents = result.getEvents();

        // --- handled IDs first pass ---
        Set<String> handledInduErrorIds = new HashSet<>();
        Set<String> handledCompensationIds = new HashSet<>();

        for (ResolvedEvent resolved : resolvedEvents) {
            RecordedEvent event = resolved.getEvent();
            String type = event.getEventType();

            if ("InduErrorHandledEvent".equals(type)) {
                handledInduErrorIds.add(deserializeEvent(event, InduErrorHandledEvent.class).getInduErrorId());
            } else if ("CompensationHandledEvent".equals(type)) {
                handledCompensationIds.add(deserializeEvent(event, CompensationHandledEvent.class).getCompensationId());
            }
        }

        // --- filter events second pass ---
        List<StreamEventDTO> filteredEvents = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault());

        Long fromEpoch = (fromDate != null && !fromDate.isBlank()) ?
                java.time.Instant.parse(fromDate).toEpochMilli() : null;
        Long toEpoch = (toDate != null && !toDate.isBlank()) ?
                java.time.Instant.parse(toDate).toEpochMilli() : null;

        for (ResolvedEvent resolved : resolvedEvents) {
            RecordedEvent event = resolved.getEvent();
            String type = event.getEventType();

            // Filter by eventType if provided
            if (eventType != null && !eventType.isBlank() && !eventType.equals(type)) {
                continue;
            }

            // Date filtering
            long eventMillis = event.getCreated().toEpochMilli();
            if ((fromEpoch != null && eventMillis < fromEpoch) ||
                    (toEpoch != null && eventMillis > toEpoch)) {
                continue; // skip event outside range
            }

            try {
                Class<?> eventClass = switch (type) {
                    case "InduErrorCreatedEvent" -> InduErrorCreatedEvent.class;
                    case "InduErrorHandledEvent" -> InduErrorHandledEvent.class;
                    case "CompensationCreatedEvent" -> CompensationCreatedEvent.class;
                    case "CompensationHandledEvent" -> CompensationHandledEvent.class;
                    case "InduErrorCurrentStateEvent" -> InduErrorCurrentStateEvent.class;
                    default -> null;
                };
                if (eventClass == null) continue;

                Object payload = deserializeEvent(event, eventClass);

                boolean include = switch (filter) {
                    case ALL -> true;
                    case ONLY_HANDLED -> switch (type) {
                        case "InduErrorCreatedEvent" -> handledInduErrorIds.contains(((InduErrorCreatedEvent) payload).getInduErrorId());
                        case "CompensationCreatedEvent" -> handledCompensationIds.contains(((CompensationCreatedEvent) payload).getCompensationId());
                        case "InduErrorCurrentStateEvent" -> true; // <-- inclure toujours
                        default -> false;
                    };
                    case ONLY_UNHANDLED -> switch (type) {
                        case "InduErrorCreatedEvent" -> !handledInduErrorIds.contains(((InduErrorCreatedEvent) payload).getInduErrorId());
                        case "CompensationCreatedEvent" -> !handledCompensationIds.contains(((CompensationCreatedEvent) payload).getCompensationId());
                        default -> false;
                    };
                };

                if (include) {
                    String createdAt = formatter.format(event.getCreated());
                    filteredEvents.add(new StreamEventDTO(type, payload, createdAt));
                }

            } catch (Exception ex) {
                log.error("❌ Failed to deserialize event {}: {}", type, ex.getMessage());
            }
        }

        // --- newest first + pagination ---
        Collections.reverse(filteredEvents);

        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, filteredEvents.size());

        if (fromIndex >= filteredEvents.size()) {
            return Collections.emptyList();
        }

        return filteredEvents.subList(fromIndex, toIndex);
    }



    private void validateUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User ID must not be null or empty.");
        }
    }

    private void validateAmount(Double amount) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0.");
        }
    }

    private <T> T deserializeEvent(RecordedEvent event, Class<T> clazz) {
        SerializedObject<byte[]> serialized = new SimpleSerializedObject<>(
                event.getEventData(),
                byte[].class,
                new SimpleSerializedType(clazz.getName(), null)
        );
        return clazz.cast(serializer.deserialize(serialized));
    }

    public boolean streamExistsForUser(String userId) {
        String streamId = "EventAggregate-" + userId;
        try {
            eventStoreDBClient.readStream(streamId, ReadStreamOptions.get().forwards().fromStart())
                    .get();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
