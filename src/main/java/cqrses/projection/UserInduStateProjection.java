package cqrses.projection;

import cqrses.event.*;
import lombok.RequiredArgsConstructor;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;

@Component
@RequiredArgsConstructor
@ProcessingGroup("userInduStateProjection")
public class UserInduStateProjection {

    private final UserInduStateRepository repository;

    @EventHandler
    public void on(InduErrorCreatedEvent event) {
        UserInduState state = repository.findById(event.getUserId())
                .orElse(UserInduState.builder()
                        .userId(event.getUserId())
                        .induErrors(new ArrayList<>())
                        .compensations(new ArrayList<>())
                        .build());

        state.getInduErrors().add(UserInduState.InduErrorInfo.builder()
                .induErrorId(event.getInduErrorId())
                .amount(event.getAmount())
                .status(event.getStatus().name())
                .createdAt(Instant.now())
                .handledAt(null)
                .build());

        state.recalcTotals();
        repository.save(state);
    }

    @EventHandler
    public void on(InduErrorHandledEvent event) {
        repository.findById(event.getUserId()).ifPresent(state -> {
            state.getInduErrors().stream()
                    .filter(e -> e.getInduErrorId().equals(event.getInduErrorId()))
                    .forEach(e -> {
                        e.setStatus(event.getStatus().name());
                        e.setHandledAt(Instant.now());
                    });

            state.recalcTotals();
            repository.save(state);
        });
    }

    @EventHandler
    public void on(CompensationCreatedEvent event) {
        UserInduState state = repository.findById(event.getUserId())
                .orElse(UserInduState.builder()
                        .userId(event.getUserId())
                        .induErrors(new ArrayList<>())
                        .compensations(new ArrayList<>())
                        .build());

        state.getCompensations().add(UserInduState.CompensationInfo.builder()
                .compensationId(event.getCompensationId())
                .amount(event.getAmount())
                .status(event.getStatus().name())
                .createdAt(Instant.now())
                .handledAt(null)
                .build());

        state.recalcTotals();
        repository.save(state);
    }

    @EventHandler
    public void on(CompensationHandledEvent event) {
        repository.findById(event.getUserId()).ifPresent(state -> {
            state.getCompensations().stream()
                    .filter(c -> c.getCompensationId().equals(event.getCompensationId()))
                    .forEach(c -> {
                        c.setStatus(event.getStatus().name());
                        c.setHandledAt(Instant.now());
                    });

            state.recalcTotals();
            repository.save(state);
        });
    }

    @EventHandler
    public void on(InduErrorCurrentStateEvent event) {
        repository.findById(event.getUserId()).ifPresent(state -> {
            state.setNetTotal(event.getTotalUntreatedAmount());
            repository.save(state);
        });
    }
}
