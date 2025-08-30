package cqrses.config;

import com.eventstore.dbclient.*;
import com.eventstore.dbclient.EventData;
import org.axonframework.eventhandling.*;
import org.axonframework.eventsourcing.eventstore.*;
import org.axonframework.serialization.Serializer;
import org.axonframework.serialization.SerializedObject;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import cqrses.event.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Primary
@Component
public class CustomEventStoreDBStorageEngine extends AbstractEventStorageEngine {

    private final EventStoreDBClient client;
    private final Serializer serializer;
    private final ObjectMapper objectMapper;

    // Map of event type names to classes for deserialization
    private static final Map<String, Class<?>> EVENT_TYPE_MAP = Map.of(
            "InduErrorCreatedEvent", InduErrorCreatedEvent.class,
            "InduErrorHandledEvent", InduErrorHandledEvent.class,
            "InduCreatedEvent", InduCreatedEvent.class,
            "InduGroupCreatedEvent", InduGroupCreatedEvent.class,
            "InduErrorCurrentStateEvent", InduErrorCurrentStateEvent.class,
            "CompensationCreatedEvent", CompensationCreatedEvent.class,
            "CompensationHandledEvent", CompensationHandledEvent.class
    );

    public CustomEventStoreDBStorageEngine(
            EventStoreDBClient client,
            @Qualifier("axonSerializer") Serializer serializer,
            ObjectMapper objectMapper
    ) {
        super(new AbstractEventStorageEngine.Builder() {});
        this.client = client;
        this.serializer = serializer;
        this.objectMapper = objectMapper;
    }

    private String getStreamName(String aggregateId) {
        return "EventAggregate-" + aggregateId;
    }

    @Override
    protected void appendEvents(List<? extends EventMessage<?>> events, Serializer ignored) {
        for (EventMessage<?> event : events) {
            if (event instanceof DomainEventMessage<?> domainEvent) {
                SerializedObject<byte[]> serializedPayload = serializer.serialize(domainEvent.getPayload(), byte[].class);
                SerializedObject<byte[]> serializedMetadata = serializer.serialize(domainEvent.getMetaData(), byte[].class);

                String streamName = getStreamName(domainEvent.getAggregateIdentifier());

                EventData eventData = EventData.builderAsJson(
                        domainEvent.getPayloadType().getSimpleName(),
                        serializedPayload.getData()
                ).metadataAsBytes(serializedMetadata.getData()).build();

                client.appendToStream(streamName, eventData).join();

                System.out.println("📥 Written event to stream " + streamName + ": " + domainEvent.getPayloadType().getSimpleName());
            }
        }
    }

    @Override
    public DomainEventStream readEvents(String aggregateIdentifier) {
        String streamName = getStreamName(aggregateIdentifier);
        ReadResult result;
        try {
            result = client.readStream(streamName, ReadStreamOptions.get().forwards().fromStart()).join();
        } catch (Exception e) {
            return DomainEventStream.of(Stream.empty());
        }

        AtomicLong sequence = new AtomicLong(0);

        return DomainEventStream.of(
                result.getEvents().stream().map(resolved -> {
                    RecordedEvent re = resolved.getEvent();
                    Class<?> eventClass = EVENT_TYPE_MAP.get(re.getEventType());
                    if (eventClass == null) {
                        throw new RuntimeException("Unknown event type: " + re.getEventType());
                    }

                    try {
                        Object payload = objectMapper.readValue(re.getEventData(), eventClass);

                        return new GenericDomainEventMessage<>(
                                re.getEventType(),
                                aggregateIdentifier,
                                sequence.getAndIncrement(),
                                payload
                        );
                    } catch (Exception ex) {
                        throw new RuntimeException("Failed to deserialize event: " + re.getEventType(), ex);
                    }
                })
        );
    }

    @Override
    protected void storeSnapshot(DomainEventMessage<?> snapshot, Serializer ignored) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(snapshot.getPayload());
            String jsonMetadata = objectMapper.writeValueAsString(snapshot.getMetaData());

            EventData eventData = EventData.builderAsJson(
                    snapshot.getPayloadType().getSimpleName(),
                    jsonPayload
            ).metadataAsJson(jsonMetadata).build();

            String streamName = getStreamName(snapshot.getAggregateIdentifier()) + "-snapshot";
            client.appendToStream(streamName, eventData).join();

            System.out.println("💾 Stored snapshot for " + snapshot.getAggregateIdentifier());
        } catch (Exception e) {
            throw new RuntimeException("Failed to store snapshot for " + snapshot.getAggregateIdentifier(), e);
        }
    }

    @Override
    protected Stream<? extends DomainEventData<?>> readEventData(String aggregateIdentifier, long firstSequenceNumber) {
        return Stream.empty();
    }

    @Override
    protected Stream<? extends TrackedEventData<?>> readEventData(TrackingToken trackingToken, boolean mayBlock) {
        return Stream.empty();
    }

    @Override
    protected Stream<? extends DomainEventData<?>> readSnapshotData(String aggregateIdentifier) {
        return Stream.empty();
    }

    // Convenience method for your business logic
    public Double getLastInduErrorAmount(String userId) {
        return this.readEvents(userId).asStream()
                .map(EventMessage::getPayload)
                .filter(InduErrorCurrentStateEvent.class::isInstance)
                .map(InduErrorCurrentStateEvent.class::cast)
                .reduce((first, second) -> second)
                .map(InduErrorCurrentStateEvent::getTotalUntreatedAmount)
                .orElse(null);
    }
}
