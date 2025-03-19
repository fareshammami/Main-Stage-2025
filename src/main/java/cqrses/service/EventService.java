package cqrses.service;

import cqrses.command.CreateEventCommand;
import lombok.RequiredArgsConstructor;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class EventService {

    private final CommandGateway commandGateway;

    public CompletableFuture<String> createEvent(String id, String data) {
        return commandGateway.send(new CreateEventCommand(id, data));
    }
}