package cqrses.config;

import com.eventstore.dbclient.*;
import com.eventstore.dbclient.EventData;
import org.axonframework.eventhandling.*;
import org.axonframework.eventsourcing.eventstore.*;
import org.axonframework.serialization.Serializer;
import org.axonframework.serialization.json.JacksonSerializer;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CustomEventStoreDBStorageEngine extends AbstractEventStorageEngine {
    private final EventStoreDBClient client;
    private final Serializer serializer;

    public CustomEventStoreDBStorageEngine(EventStoreDBClient client, Serializer serializer) {
        super(new AbstractEventStorageEngine.Builder() {});
        this.client = client;
        this.serializer = serializer;
    }

    @Override
    protected void appendEvents(List<? extends EventMessage<?>> events, Serializer serializer) {
        events.forEach(event -> {
            byte[] payload = serializer.serialize(event.getPayload(), byte[].class).getData();
            byte[] metadata = serializer.serialize(event.getMetaData(), byte[].class).getData();

            EventData eventData = EventData.builderAsBinary(event.getPayloadType().getSimpleName(), payload)
                    .metadataAsBytes(metadata)
                    .build();

            client.appendToStream(event.getIdentifier(), eventData).join();
        });
    }

    @Override
    public DomainEventStream readEvents(String aggregateIdentifier) {
        ReadStreamOptions options = ReadStreamOptions.get().forwards().fromStart();
        ReadResult result = client.readStream(aggregateIdentifier, options).join();

        Stream<ResolvedEvent> events = result.getEvents().stream();
        return DomainEventStream.of(events.map(event -> new GenericDomainEventMessage<>(
                event.getEvent().getEventType(),
                aggregateIdentifier,
                event.getEvent().getRevision(),
                event.getEvent().getEventData()
        )));
    }

    @Override
    protected void storeSnapshot(DomainEventMessage<?> snapshot, Serializer serializer) {
        byte[] snapshotData = serializer.serialize(snapshot.getPayload(), byte[].class).getData();

        EventData eventData = EventData.builderAsBinary(snapshot.getPayloadType().getSimpleName(), snapshotData)
                .metadataAsBytes(serializer.serialize(snapshot.getMetaData(), byte[].class).getData())
                .build();

        client.appendToStream(snapshot.getAggregateIdentifier() + "-snapshot", eventData).join();
    }

    @Override
    protected Stream<? extends DomainEventData<?>> readEventData(String aggregateIdentifier, long firstSequenceNumber) {
        return Stream.empty(); // Implement this properly for event retrieval.
    }

    @Override
    protected Stream<? extends TrackedEventData<?>> readEventData(TrackingToken trackingToken, boolean mayBlock) {
        return Stream.empty();
    }

    @Override
    protected Stream<? extends DomainEventData<?>> readSnapshotData(String aggregateIdentifier) {
        return Stream.empty();
    }
}
