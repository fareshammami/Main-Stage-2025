package cqrses.aggregate;

import cqrses.command.CreateEventCommand;
import cqrses.command.UpdateEventCommand;
import cqrses.event.CreateEvent;
import cqrses.event.UpdateEvent;
import lombok.NoArgsConstructor;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.spring.stereotype.Aggregate;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

@Aggregate
@NoArgsConstructor
public class EventAggregate {

    @AggregateIdentifier
    private String id;
    private String data;

    @CommandHandler
    public EventAggregate(CreateEventCommand command) {
        apply(new CreateEvent(command.getId(), command.getData()));
    }

    @EventSourcingHandler
    public void on(CreateEvent event) {
        System.out.println("✅ CreateEvent REJOUÉ : id=" + event.getId() + ", data=" + event.getData());
        this.id = event.getId();
        this.data = event.getData();
    }

    @CommandHandler
    public void handle(UpdateEventCommand command) {
        apply(new UpdateEvent(command.getId(), command.getNewMessage()));
    }

    @EventSourcingHandler
    public void on(UpdateEvent event) {
        System.out.println("🔄 UpdateEvent REJOUÉ : newMessage=" + event.getNewMessage());
        this.data = event.getNewMessage();
    }
}
