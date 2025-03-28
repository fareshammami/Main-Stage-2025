package cqrses.config;

import com.eventstore.dbclient.*;
import org.axonframework.commandhandling.CommandBus;
import org.axonframework.commandhandling.SimpleCommandBus;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.commandhandling.gateway.DefaultCommandGateway;
import org.axonframework.config.Configurer;
import org.axonframework.eventsourcing.eventstore.*;
import org.axonframework.serialization.Serializer;
import org.axonframework.serialization.json.JacksonSerializer;
import org.axonframework.spring.eventsourcing.SpringAggregateSnapshotter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class AxonEventStoreConfig {

    @Bean
    public EventStoreDBClient eventStoreDBClient() {
        return EventStoreDBClient.create(EventStoreDBConnectionString.parseOrThrow("esdb://localhost:2113?tls=false"));
    }

    @Bean
    public EventStorageEngine eventStorageEngine(EventStoreDBClient client, @Qualifier("eventSerializer") Serializer serializer) {
        return new CustomEventStoreDBStorageEngine(client, serializer);
    }

    @Bean
    public EventStore eventStore(EventStorageEngine storageEngine) {
        return EmbeddedEventStore.builder().storageEngine(storageEngine).build();
    }

    @Bean
    public SpringAggregateSnapshotter snapshotter(EventStore eventStore) {
        return SpringAggregateSnapshotter.builder()
                .eventStore(eventStore)
                .executor(Runnable::run)
                .build();
    }

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
}
