package cqrses.aggregate;

import cqrses.command.CreateEventCommand;
import cqrses.command.UpdateEventCommand;
import cqrses.event.CreateEvent;
import cqrses.event.UpdateEvent;
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class EventAggregateTest {

    private AggregateTestFixture<EventAggregate> fixture;

    @BeforeEach
    void setUp() {
        fixture = new AggregateTestFixture<>(EventAggregate.class);
    }

    @Test
    void testCreateEventCommand() {
        fixture.givenNoPriorActivity()
                .when(new CreateEventCommand("event2", "initial data"))
                .expectSuccessfulHandlerExecution()
                .expectEvents(new CreateEvent("event2", "initial data"));
    }

    @Test
    void testUpdateEventCommand() {
        fixture.given(new CreateEvent("event2", "initial data"))
                .when(new UpdateEventCommand("event2", "updated data"))
                .expectSuccessfulHandlerExecution()
                .expectEvents(new UpdateEvent("event2", "updated data"));
    }
}
