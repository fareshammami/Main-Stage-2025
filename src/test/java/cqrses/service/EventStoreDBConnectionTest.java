package cqrses.service;

import com.eventstore.dbclient.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class EventStoreDBConnectionTest {

    private EventStoreDBClient client;

    @BeforeEach
    void setup() {
        EventStoreDBClientSettings connectionString =
                EventStoreDBConnectionString.parseOrThrow("esdb://localhost:2113?tls=false");
        client = EventStoreDBClient.create(connectionString);
    }

    @Test
    void testAppendAndReadEvent() throws Exception {
        String streamId = "test-stream-" + UUID.randomUUID();

        EventData eventData = EventData.builderAsJson("TestEvent", "{\"message\":\"Hello EventStore\"}".getBytes())
                .build();

        client.appendToStream(streamId, eventData).join();

        ReadResult result = client.readStream(streamId, ReadStreamOptions.get().forwards().fromStart()).get();

        List<ResolvedEvent> events = result.getEvents();

        assertFalse(events.isEmpty());
        String jsonData = new String(events.get(0).getEvent().getEventData());
        assertTrue(jsonData.contains("Hello EventStore"));
    }
}
