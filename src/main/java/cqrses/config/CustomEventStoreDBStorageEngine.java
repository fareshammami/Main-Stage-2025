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
            //  Cast vers DomainEventMessage pour avoir access au aggregateIdentifier
            if (event instanceof DomainEventMessage<?>) {
                DomainEventMessage<?> domainEvent = (DomainEventMessage<?>) event;

                byte[] payload = serializer.serialize(domainEvent.getPayload(), byte[].class).getData();
                byte[] metadata = serializer.serialize(domainEvent.getMetaData(), byte[].class).getData();

                // Nom du stream cohérent basé sur le type + aggregateId
                String streamName = domainEvent.getType() + "-" + domainEvent.getAggregateIdentifier();

                String jsonPayload = new String(payload); // le payload a déjà été encodé en JSON par le serializer
                String jsonMetadata = new String(metadata);

                EventData eventData = EventData.builderAsJson(domainEvent.getPayloadType().getSimpleName(), jsonPayload)
                        .metadataAsJson(jsonMetadata)
                        .build();

                client.appendToStream(streamName, eventData).join();
            }
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
