package cqrses.config;

import com.eventstore.dbclient.*;
import org.axonframework.eventhandling.*;
import org.axonframework.eventsourcing.eventstore.AbstractEventStorageEngine;
import org.axonframework.eventsourcing.eventstore.DomainEventStream;
import org.axonframework.serialization.*;
import org.axonframework.serialization.json.JacksonSerializer;
import com.eventstore.dbclient.EventData;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class CustomEventStoreDBStorageEngine extends AbstractEventStorageEngine {
    private final EventStoreDBClient client;
    private final Serializer serializer; // Use injected serializer

    public CustomEventStoreDBStorageEngine(EventStoreDBClient client, Serializer serializer) {
        super((Builder) serializer);
        this.client = client;
        this.serializer = serializer; // Store the injected serializer
    }


    @Override
    protected void appendEvents(List<? extends EventMessage<?>> events, Serializer serializer) {
        events.forEach(event -> {
            // Serialize metadata and payload
            SerializedObject<byte[]> metadataSerialized = serializer.serialize(event.getMetaData(), byte[].class);
            SerializedObject<byte[]> payloadSerialized = serializer.serialize(event.getPayload(), byte[].class);

            byte[] metadata = metadataSerialized.getData();
            byte[] payload = payloadSerialized.getData();

            // Create the event data and append to the EventStoreDB stream
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

        return DomainEventStream.of(events.map(event ->
                new GenericDomainEventMessage<>(
                        event.getEvent().getEventType(),
                        aggregateIdentifier,
                        event.getEvent().getRevision(),
                        event.getEvent().getEventData()
                )
        ));
    }

    @Override
    protected void storeSnapshot(DomainEventMessage<?> snapshot, Serializer serializer) {
        // Serialize snapshot payload
        SerializedObject<byte[]> serializedSnapshot = serializer.serialize(snapshot.getPayload(), byte[].class);
        byte[] snapshotData = serializedSnapshot.getData();

        // Create the snapshot event data and append to EventStoreDB stream
        EventData eventData = EventData.builderAsBinary(snapshot.getPayloadType().getSimpleName(), snapshotData)
                .metadataAsBytes(serializer.serialize(snapshot.getMetaData(), byte[].class).getData())
                .build();

        client.appendToStream(snapshot.getAggregateIdentifier() + "-snapshot", eventData).join();
    }

    @Override
    protected Stream<? extends DomainEventData<?>> readEventData(String s, long l) {
        return null;
    }

    @Override
    protected Stream<? extends TrackedEventData<?>> readEventData(TrackingToken trackingToken, boolean b) {
        return null;
    }

    @Override
    protected Stream<? extends DomainEventData<?>> readSnapshotData(String s) {
        return null;
    }


}


