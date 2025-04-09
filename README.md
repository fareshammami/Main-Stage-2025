# Main-Stage-2025
@Override
protected void appendEvents(List<? extends EventMessage<?>> events, Serializer serializer) {
    events.forEach(event -> {
        // ⚠️ Cast vers DomainEventMessage pour avoir access au aggregateIdentifier
        if (event instanceof DomainEventMessage<?>) {
            DomainEventMessage<?> domainEvent = (DomainEventMessage<?>) event;

            byte[] payload = serializer.serialize(domainEvent.getPayload(), byte[].class).getData();
            byte[] metadata = serializer.serialize(domainEvent.getMetaData(), byte[].class).getData();

            // 🧠 Nom du stream cohérent basé sur le type + aggregateId
            String streamName = domainEvent.getType() + "-" + domainEvent.getAggregateIdentifier();

            EventData eventData = EventData.builderAsBinary(domainEvent.getPayloadType().getSimpleName(), payload)
                    .metadataAsBytes(metadata)
                    .build();

            client.appendToStream(streamName, eventData).join();
        }
    });
}
