package cqrses.rest;

import cqrses.projection.UserInduState;
import cqrses.projection.UserInduStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/indu-errors/state")
@RequiredArgsConstructor
@CrossOrigin("*")
public class UserInduStateController {

    private final UserInduStateRepository repository;

    @GetMapping("/{userId}")
    public ResponseEntity<UserInduState> getUserInduState(@PathVariable String userId) {
        return repository.findById(userId)
                .map(state -> {
                    state.recalcTotals(); // ensure totals are up-to-date
                    return ResponseEntity.ok(state);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
