package cqrses.rest;

import cqrses.dto.UserDto;
import cqrses.entity.User;
import cqrses.repository.UserRepository;
import cqrses.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@CrossOrigin("*")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    @GetMapping
    public Page<UserDto> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return userService.getAllUsers(page, size);
    }

    @GetMapping("/search")
    public Page<UserDto> searchUsers(
            @RequestParam String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return userService.searchUsersByEmail(email, page, size);
    }

    @GetMapping("/search-by-amount")
    public Page<UserDto> searchByAmount(
            @RequestParam double maxAmount,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return userService.getUsersByMaxAmount(maxAmount, page, size);
    }

    @GetMapping("/{userId}/username")
    public Map<String, String> getUsernameByUserId(@PathVariable String userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        return Map.of("username", userOpt.map(User::getUsername).orElse("N/A"));
    }

    @GetMapping("/search-combined")
    public Page<UserDto> searchUsersCombined(
            @RequestParam String email,
            @RequestParam double maxAmount,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return userService.searchUsersByEmailAndMaxAmount(email, maxAmount, page, size);
    }
}
