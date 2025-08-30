package cqrses.config;

import com.eventstore.dbclient.EventStoreDBClient;
import com.eventstore.dbclient.EventStoreDBConnectionString;
import org.axonframework.commandhandling.CommandBus;
import org.axonframework.commandhandling.SimpleCommandBus;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.commandhandling.gateway.DefaultCommandGateway;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.axonframework.eventsourcing.eventstore.EmbeddedEventStore;
import org.axonframework.eventsourcing.eventstore.EventStorageEngine;
import org.axonframework.spring.eventsourcing.SpringAggregateSnapshotter;
import org.axonframework.serialization.Serializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AxonEventStoreConfig {

    // ---------------- EventStoreDB Client ----------------
    @Bean
    public EventStoreDBClient eventStoreDBClient() {
        return EventStoreDBClient.create(EventStoreDBConnectionString.parseOrThrow("esdb://localhost:2113?tls=false"));
    }

    // ---------------- Event Storage Engine ----------------
    @Bean
    public EventStorageEngine eventStorageEngine(
            EventStoreDBClient client,
            @Qualifier("axonSerializer") Serializer serializer,
            CustomEventStoreDBStorageEngine customEngine
    ) {
        // Use the custom storage engine
        return customEngine;
    }

    // ---------------- Event Store ----------------
    @Bean
    public EventStore eventStore(EventStorageEngine storageEngine) {
        return EmbeddedEventStore.builder()
                .storageEngine(storageEngine)
                .build();
    }

    // ---------------- Snapshotter ----------------
    @Bean
    public SpringAggregateSnapshotter snapshotter(EventStore eventStore) {
        return SpringAggregateSnapshotter.builder()
                .eventStore(eventStore)
                .executor(Runnable::run) // synchronous execution
                .build();
    }

    // ---------------- Command Bus & Gateway ----------------
    @Bean
    public CommandBus commandBus() {
        return SimpleCommandBus.builder().build();
    }

    @Bean
    public CommandGateway commandGateway(CommandBus commandBus) {
        return DefaultCommandGateway.builder()
                .commandBus(commandBus)
                .build();
    }

    // ---------------- Serializer ----------------
    // Remove this bean to avoid conflicts with JacksonConfig
    // We now rely on the @Primary axonSerializer from JacksonConfig
}
