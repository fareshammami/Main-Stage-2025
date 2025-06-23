package cqrses.aggregate;

import cqrses.command.*;
import cqrses.entity.CompensationStatus;
import cqrses.entity.InduErrorStatus;
import cqrses.event.*;
import lombok.NoArgsConstructor;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.spring.stereotype.Aggregate;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

@Aggregate
@NoArgsConstructor
public class InduErrorAggregate {

    @AggregateIdentifier
    private String groupId;

    private Set<String> handledErrors = new HashSet<>();

    @CommandHandler
    public InduErrorAggregate(CreateInduErrorCommand command) {
        apply(new InduGroupCreatedEvent(command.getGroupId()));
    }

    @CommandHandler
    public void handle(AddInduErrorCommand command) {
        apply(new InduErrorCreatedEvent(
                UUID.randomUUID().toString(),
                command.getAmount(),
                InduErrorStatus.NOT_TRAITED
        ));
    }

    @CommandHandler
    public void handle(HandleInduErrorCommand command) {
        if (!handledErrors.contains(command.getInduErrorId())) {
            apply(new InduErrorHandledEvent(
                    command.getInduErrorId(),
                    InduErrorStatus.TRAITED
            ));
        }
    }
    @CommandHandler
    public void handle(ProcessInduErrorsCommand command) {
        apply(new InduErrorCurrentStateEvent(
                command.getGroupId(),
                command.getTotalUntreatedAmount()
        ));
    }
    @CommandHandler
    public void handle(AddCompensationCommand command) {
        apply(new CompensationCreatedEvent(
                UUID.randomUUID().toString(),
                command.getAmount(),
                CompensationStatus.NOT_TRAITED
        ));
    }
    @CommandHandler
    public void handle(HandleCompensationCommand command) {
        apply(new CompensationHandledEvent(
                command.getCompensationId(),
                CompensationStatus.TRAITED
        ));
    }

    @EventSourcingHandler
    public void on(InduGroupCreatedEvent event) {
        this.groupId = event.getGroupId();
    }

    @EventSourcingHandler
    public void on(InduErrorCreatedEvent event) {
        // Optional: handle creation
    }

    @EventSourcingHandler
    public void on(InduErrorHandledEvent event) {
        handledErrors.add(event.getInduErrorId());
    }

    @EventSourcingHandler
    public void on(InduErrorCurrentStateEvent event) {
        System.out.println("📊 Current untreated amount for group " + event.getGroupId() + ": " + event.getTotalUntreatedAmount());
        // Optionally, store the amount if needed
    }

    @EventSourcingHandler
    public void on(CompensationCreatedEvent event) {
        // Optionnel
    }
    @EventSourcingHandler
    public void on(CompensationHandledEvent event) {
        // Log or track handled compensations if needed
        System.out.println("✅ Compensation traitée : " + event.getCompensationId());
    }
}
