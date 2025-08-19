    package cqrses.config;

    import com.eventstore.dbclient.*;
    import com.eventstore.dbclient.EventData;
    import cqrses.event.*;
    import org.axonframework.eventhandling.*;
    import org.axonframework.eventsourcing.eventstore.*;
    import org.axonframework.serialization.SerializedObject;
    import org.axonframework.serialization.Serializer;
    import org.axonframework.serialization.SimpleSerializedObject;
    import org.axonframework.serialization.SimpleSerializedType;
    import org.axonframework.eventhandling.GenericDomainEventMessage;
    import org.springframework.context.annotation.Bean;
    import org.springframework.context.annotation.Primary;
    import org.springframework.stereotype.Component;

    import java.util.Comparator;
    import java.util.List;
    import java.util.concurrent.atomic.AtomicLong;
    import java.util.stream.Collectors;
    import java.util.stream.Stream;
    @Primary
    @Component

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
        protected void appendEvents(List<? extends EventMessage<?>> events, Serializer ignored) {
            events.forEach(event -> {
                if (event instanceof DomainEventMessage<?> domainEvent) {
                    // Sérialisation correcte en byte[]
                    SerializedObject<byte[]> serializedPayload = this.serializer.serialize(domainEvent.getPayload(), byte[].class);
                    SerializedObject<byte[]> serializedMetadata = this.serializer.serialize(domainEvent.getMetaData(), byte[].class);

                    String streamName = getStreamName(domainEvent.getAggregateIdentifier());

                    System.out.println("📥 Writing to stream: " + streamName);
                    System.out.println("📦 Event type: " + domainEvent.getPayloadType().getSimpleName());

                    EventData eventData = EventData.builderAsJson(
                            domainEvent.getPayloadType().getSimpleName(),
                            serializedPayload.getData()
                    ).metadataAsBytes(serializedMetadata.getData()).build();

                    client.appendToStream(streamName, eventData).join();
                }
            });
        }

        @Override
        public DomainEventStream readEvents(String aggregateIdentifier) {
            String streamName = getStreamName(aggregateIdentifier);
            System.out.println("📤 Reading stream: " + streamName);

            ReadResult result = client.readStream(streamName, ReadStreamOptions.get().forwards().fromStart()).join();
            AtomicLong sequence = new AtomicLong(0);

            return DomainEventStream.of(result.getEvents().stream().map(resolved -> {
                RecordedEvent re = resolved.getEvent();

                System.out.println("✅ Event found: " + re.getEventType());

                Class<?> eventClass = switch (re.getEventType()) {
                    case "InduErrorCreatedEvent" -> InduErrorCreatedEvent.class;
                    case "InduErrorHandledEvent" -> InduErrorHandledEvent.class;
                    case "InduCreatedEvent" -> InduCreatedEvent.class;
                    case "InduGroupCreatedEvent" -> InduGroupCreatedEvent.class;
                    case "InduErrorCurrentStateEvent" -> InduErrorCurrentStateEvent.class;
                    case "CompensationCreatedEvent" -> CompensationCreatedEvent.class;
                    case "CompensationHandledEvent" -> CompensationHandledEvent.class;
                    default -> throw new RuntimeException("Unknown event: " + re.getEventType());
                };

                SerializedObject<byte[]> serializedObject = new SimpleSerializedObject<>(
                        re.getEventData(),
                        byte[].class,
                        new SimpleSerializedType(eventClass.getName(), null)
                );

                Object payload = serializer.deserialize(serializedObject);

                return new GenericDomainEventMessage<>(
                        re.getEventType(),              // type
                        aggregateIdentifier,           // aggregate ID
                        sequence.getAndIncrement(),    // sequence
                        payload                        // payload
                );
            }));
        }




        @Override
        protected void storeSnapshot(DomainEventMessage<?> snapshot, Serializer serializer) {
            String jsonPayload = serializer.serialize(snapshot.getPayload(), String.class).getData();
            String jsonMetadata = serializer.serialize(snapshot.getMetaData(), String.class).getData();

            EventData eventData = EventData.builderAsJson(
                    snapshot.getPayloadType().getSimpleName(),
                    jsonPayload
            ).metadataAsJson(jsonMetadata).build();

            String streamName = getStreamName(snapshot.getAggregateIdentifier()) + "-snapshot";
            client.appendToStream(streamName, eventData).join();
        }
        public Double getLastInduErrorAmount(String userId) {
            try {
                // Read all events for the given user (aggregate)
                List<InduErrorCurrentStateEvent> events = this.readEvents(userId).asStream()
                        .map(eventMessage -> eventMessage.getPayload()) // payload is the actual event object
                        .filter(payload -> payload instanceof InduErrorCurrentStateEvent)
                        .map(payload -> (InduErrorCurrentStateEvent) payload)
                        .collect(Collectors.toList());

                if (!events.isEmpty()) {
                    // Take the last event in the list
                    return events.get(events.size() - 1).getTotalUntreatedAmount();
                }
                return null; // no events, return null
            } catch (Exception e) {
                // Any error while reading events, just return null
                System.err.println("Failed to read events for user " + userId + ": " + e.getMessage());
                return null;
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
    }
