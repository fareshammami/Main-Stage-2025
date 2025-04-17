package cqrses.config;

import com.eventstore.dbclient.*;
import com.eventstore.dbclient.EventData;
import cqrses.event.CreateEvent;
import cqrses.event.UpdateEvent;
import org.axonframework.eventhandling.*;
import org.axonframework.eventsourcing.eventstore.*;
import org.axonframework.serialization.SerializedObject;
import org.axonframework.serialization.Serializer;
import org.axonframework.serialization.SimpleSerializedObject;
import org.axonframework.serialization.SimpleSerializedType;
import org.axonframework.serialization.json.JacksonSerializer;
import java.util.concurrent.atomic.AtomicLong;

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

    private String getStreamName(String aggregateId) {
        return "EventAggregate-" + aggregateId;
    }


    @Override
    protected void appendEvents(List<? extends EventMessage<?>> events, Serializer serializer) {
        events.forEach(event -> {
            if (event instanceof DomainEventMessage<?> domainEvent) {

                String jsonPayload = serializer.serialize(domainEvent.getPayload(), String.class).getData();
                String jsonMetadata = serializer.serialize(domainEvent.getMetaData(), String.class).getData();

                String streamName = getStreamName(domainEvent.getAggregateIdentifier());


                System.out.println(" Writing to stream: " + streamName);
                System.out.println(" Event type: " + domainEvent.getPayloadType().getSimpleName());
                System.out.println(" Payload: " + jsonPayload);
                System.out.println(" Metadata: " + jsonMetadata);

                EventData eventData = EventData.builderAsJson(
                        domainEvent.getPayloadType().getSimpleName(),
                        jsonPayload
                ).metadataAsJson(jsonMetadata).build();

                client.appendToStream(streamName, eventData).join();
            }
        });
    }

    @Override
    public DomainEventStream readEvents(String aggregateIdentifier) {
        String streamName = getStreamName(aggregateIdentifier);


        System.out.println(" Reading stream: " + streamName);

        ReadResult result = client.readStream(streamName, ReadStreamOptions.get().forwards().fromStart()).join();

        AtomicLong sequence = new AtomicLong(0);

        return DomainEventStream.of(result.getEvents().stream().map(resolved -> {
            RecordedEvent re = resolved.getEvent();

            //  LOG AJOUTÉ
            System.out.println(" Event found: " + re.getEventType());

            Class<?> eventClass = switch (re.getEventType()) {
                case "CreateEvent" -> CreateEvent.class;
                case "UpdateEvent" -> UpdateEvent.class;
                default -> throw new RuntimeException("Unknown event: " + re.getEventType());
            };

            SerializedObject<byte[]> serializedObject = new SimpleSerializedObject<>(
                    re.getEventData(), byte[].class, new SimpleSerializedType(eventClass.getName(), null)
            );

            Object payload = serializer.deserialize(serializedObject);

            return new GenericDomainEventMessage<>(
                    re.getEventType(),
                    aggregateIdentifier,
                    sequence.getAndIncrement(),
                    payload
            );
        }));
    }


    @Override
    protected void storeSnapshot(DomainEventMessage<?> snapshot, Serializer serializer) {
        byte[] payloadBytes = serializer.serialize(snapshot.getPayload(), byte[].class).getData();
        byte[] metadataBytes = serializer.serialize(snapshot.getMetaData(), byte[].class).getData();

        String payload = new String(payloadBytes);
        String metadata = new String(metadataBytes);

        EventData eventData = EventData.builderAsJson(
                        snapshot.getPayloadType().getSimpleName(),
                        payload
                )
                .metadataAsJson(metadata)
                .build();

        String streamName = "EventAggregate-" + snapshot.getAggregateIdentifier() + "-snapshot";

        client.appendToStream(streamName, eventData).join();
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
