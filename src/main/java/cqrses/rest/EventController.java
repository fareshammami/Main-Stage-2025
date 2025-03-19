package cqrses.rest;

import cqrses.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/event")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @PostMapping("/create")
    public CompletableFuture<ResponseEntity<String>> createEvent(@RequestParam String id, @RequestParam String data) {
        return eventService.createEvent(id, data)
                .thenApply(result -> ResponseEntity.ok("Event created with ID: " + result));
    }
}
