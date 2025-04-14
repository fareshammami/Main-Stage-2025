package cqrses.service;

import com.eventstore.dbclient.EventStoreDBClient;
import com.eventstore.dbclient.ReadResult;
import com.eventstore.dbclient.ReadStreamOptions;
import cqrses.command.CreateEventCommand;
import cqrses.command.UpdateEventCommand;
import lombok.RequiredArgsConstructor;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventService {

    private final CommandGateway commandGateway;
    private final EventStoreDBClient eventStoreDBClient;

    public CompletableFuture<String> createEvent(String id, String data) {
        return commandGateway.send(new CreateEventCommand(id, data));
    }

    public List<String> getEventsFromEventStore(String streamId) throws Exception {
        ReadStreamOptions options = ReadStreamOptions.get().forwards().fromStart();
        ReadResult result = eventStoreDBClient.readStream(streamId, options).get();

        return result.getEvents().stream()
                .map(e -> new String(e.getEvent().getEventData())) //
                .collect(Collectors.toList());
    }

    public CompletableFuture<String> updateEvent(String id, String newMessage) {
        return commandGateway.send(new UpdateEventCommand(id, newMessage));
    }


}