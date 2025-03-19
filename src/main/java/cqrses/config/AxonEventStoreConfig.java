package cqrses.config;

import com.eventstore.dbclient.*;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.config.DefaultConfigurer;
import org.axonframework.eventsourcing.eventstore.*;
import org.axonframework.serialization.Serializer;
import org.axonframework.serialization.json.JacksonSerializer;
import org.axonframework.spring.eventsourcing.SpringAggregateSnapshotter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AxonEventStoreConfig {

    @Bean
    public EventStoreDBClient eventStoreDBClient() {
        return EventStoreDBClient.create(EventStoreDBConnectionString.parseOrThrow("esdb://localhost:2113?tls=false"));
    }

    @Bean
    public Serializer eventSerializer() {
        return JacksonSerializer.defaultSerializer();
    }

    @Bean
    public EventStorageEngine eventStorageEngine(EventStoreDBClient client, Serializer serializer) {
        return new CustomEventStoreDBStorageEngine(client, serializer);
    }

    @Bean
    public EventStore eventStore(EventStorageEngine storageEngine) {
        return EmbeddedEventStore.builder().storageEngine(storageEngine).build();
    }

    @Bean
    public SpringAggregateSnapshotter snapshotter() {
        return SpringAggregateSnapshotter.builder().build();
    }

}
