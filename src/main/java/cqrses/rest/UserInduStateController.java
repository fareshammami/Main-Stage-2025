package cqrses.rest;

import cqrses.projection.UserInduState;
import cqrses.projection.UserInduStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/indu-errors/projection")
@RequiredArgsConstructor
public class UserInduStateController {

    private final UserInduStateRepository repository;

    @GetMapping("/{userId}")
    public ResponseEntity<UserInduState> getUserInduState(@PathVariable String userId) {
        return repository.findById(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
