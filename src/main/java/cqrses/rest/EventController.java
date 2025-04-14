package cqrses.rest;

import cqrses.command.CreateEventCommand;
import cqrses.command.UpdateEventCommand;
import cqrses.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/event")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @PostMapping("/create")
    public CompletableFuture<ResponseEntity<String>> createEvent(@RequestBody CreateEventCommand command) {
        return eventService.createEvent(command.getId(), command.getData())
                .thenApply(result -> ResponseEntity.ok("Event created with ID: " + result));
    }
    @GetMapping("/stream/{streamId}")
    public ResponseEntity<List<String>> getEventsFromStream(@PathVariable String streamId) {
        try {
            // Retrieve events using the streamId
            List<String> events = eventService.getEventsFromEventStore(streamId);
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            // In case of error, send an internal server error response
            return ResponseEntity.internalServerError().body(List.of("Error retrieving events: " + e.getMessage()));
        }
    }
    @PostMapping("/update")
    public CompletableFuture<ResponseEntity<String>> updateEvent(@RequestBody UpdateEventCommand command) {
        return eventService.updateEvent(command.getId(), command.getNewMessage())
                .thenApply(result -> ResponseEntity.ok("Event updated: " + result));
    }

}
