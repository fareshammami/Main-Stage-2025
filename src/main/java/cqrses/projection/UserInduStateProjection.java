package cqrses.projection;

import cqrses.entity.CompensationStatus;
import cqrses.event.*;
import lombok.RequiredArgsConstructor;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.config.ProcessingGroup;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@ProcessingGroup("userInduState")
public class UserInduStateProjection {

    private final UserInduStateRepository repository;

    @EventHandler
    public void on(InduErrorCreatedEvent event) {
        System.out.println("📥 Received InduErrorCreatedEvent for user: " + event.getUserId());

        UserInduState state = repository.findById(event.getUserId()).orElseGet(() -> {
            System.out.println("🆕 Creating new UserInduState for: " + event.getUserId());
            return UserInduState.builder()
                    .userId(event.getUserId())
                    .induErrors(new ArrayList<>())
                    .compensations(new ArrayList<>())
                    .build();
        });

        state.getInduErrors().add(
                UserInduState.InduErrorInfo.builder()
                        .induErrorId(event.getInduErrorId())
                        .amount(event.getAmount())
                        .status(event.getStatus().name())
                        .build()
        );

        recalcTotals(state);
        repository.save(state);
        System.out.println("✅ InduError added and projection saved.");
    }

    @EventHandler
    public void on(InduErrorHandledEvent event) {
        System.out.println("📥 Received InduErrorHandledEvent for induErrorId: " + event.getInduErrorId());

        repository.findAll().stream()
                .filter(state -> state.getInduErrors().stream()
                        .anyMatch(i -> i.getInduErrorId().equals(event.getInduErrorId())))
                .findFirst()
                .ifPresent(state -> {
                    state.getInduErrors().stream()
                            .filter(i -> i.getInduErrorId().equals(event.getInduErrorId()))
                            .forEach(i -> i.setStatus(event.getStatus().name()));

                    recalcTotals(state);
                    repository.save(state);
                    System.out.println("✅ InduError status updated and projection saved.");
                });
    }

    @EventHandler
    public void on(CompensationCreatedEvent event) {
        System.out.println("📥 Received CompensationCreatedEvent for user: " + event.getUserId());

        UserInduState state = repository.findById(event.getUserId()).orElseGet(() -> {
            System.out.println("🆕 Creating new UserInduState for: " + event.getUserId());
            return UserInduState.builder()
                    .userId(event.getUserId())
                    .induErrors(new ArrayList<>())
                    .compensations(new ArrayList<>())
                    .build();
        });

        state.getCompensations().add(
                UserInduState.CompensationInfo.builder()
                        .compensationId(event.getCompensationId())
                        .amount(event.getAmount())
                        .status(event.getStatus().name())
                        .build()
        );

        recalcTotals(state);
        repository.save(state);
        System.out.println("✅ Compensation added and projection saved.");
    }

    @EventHandler
    public void on(CompensationHandledEvent event) {
        System.out.println("📥 Received CompensationHandledEvent for compId: " + event.getCompensationId());

        repository.findAll().stream()
                .filter(state -> state.getCompensations().stream()
                        .anyMatch(c -> c.getCompensationId().equals(event.getCompensationId())))
                .findFirst()
                .ifPresent(state -> {
                    state.getCompensations().stream()
                            .filter(c -> c.getCompensationId().equals(event.getCompensationId()))
                            .forEach(c -> c.setStatus(CompensationStatus.TRAITED.name()));

                    recalcTotals(state);
                    repository.save(state);
                    System.out.println("✅ Compensation status updated and projection saved.");
                });
    }

    @EventHandler
    public void on(InduErrorCurrentStateEvent event) {
        System.out.println("📥 Received InduErrorCurrentStateEvent for user: " + event.getUserId());

        repository.findById(event.getUserId()).ifPresentOrElse(state -> {
            state.setNetTotal(event.getTotalUntreatedAmount()); // assuming this field exists
            repository.save(state);
            System.out.println("✅ Updated net total for user " + event.getUserId());
        }, () -> {
            // Optionally create a new record
            System.out.println("⚠️ No existing state found for user " + event.getUserId() + " — cannot update net total.");
        });
    }

    private void recalcTotals(UserInduState state) {
        double totalUntreatedInduErrors = state.getInduErrors().stream()
                .filter(i -> i.getStatus().equals("NOT_TRAITED"))
                .mapToDouble(UserInduState.InduErrorInfo::getAmount)
                .sum();

        double totalUntreatedCompensations = state.getCompensations().stream()
                .filter(c -> c.getStatus().equals("NOT_TRAITED"))
                .mapToDouble(UserInduState.CompensationInfo::getAmount)
                .sum();

        state.setTotalUntreatedInduErrors(totalUntreatedInduErrors);
        state.setTotalUntreatedCompensations(totalUntreatedCompensations);

        System.out.println("📊 Totals recalculated — Indus: " + totalUntreatedInduErrors +
                ", Compensations: " + totalUntreatedCompensations);
    }
}
