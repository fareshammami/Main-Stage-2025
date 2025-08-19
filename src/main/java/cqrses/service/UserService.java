package cqrses.service;

import cqrses.dto.UserDto;
import cqrses.entity.Role;
import cqrses.entity.User;
import cqrses.repository.UserRepository;
import cqrses.config.CustomEventStoreDBStorageEngine;
import lombok.RequiredArgsConstructor;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final CustomEventStoreDBStorageEngine eventStoreStorageEngine;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public boolean existsById(String userId) {
        return userRepository.existsById(userId);
    }

    public Page<UserDto> getAllUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("username").ascending());

        return userRepository.findAll(pageable)
                .map(user -> {
                    Double lastAmount = eventStoreStorageEngine.getLastInduErrorAmount(user.getId());
                    return new UserDto(
                            user.getId(),
                            user.getUsername(),
                            user.getEmail(),
                            lastAmount
                    );
                });
    }
    public Page<UserDto> searchUsersByEmail(String email, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("username").ascending());

        Page<User> usersPage = userRepository.findByEmailContainingIgnoreCase(email, pageable);

        return usersPage.map(user -> new UserDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                eventStoreStorageEngine.getLastInduErrorAmount(user.getId())
        ));
    }

    public Page<UserDto> getUsersByMaxAmount(double max, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("username").ascending());

        List<UserDto> allFiltered = userRepository.findAll().stream()
                .map(user -> {
                    Double lastAmount = eventStoreStorageEngine.getLastInduErrorAmount(user.getId());
                    if (lastAmount == null || lastAmount <= max) {
                        return new UserDto(user.getId(), user.getUsername(), user.getEmail(), lastAmount);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .toList();

        // total count BEFORE slicing
        int total = allFiltered.size();

        // apply manual pagination
        int start = Math.min(page * size, total);
        int end = Math.min(start + size, total);
        List<UserDto> pageContent = allFiltered.subList(start, end);

        return new PageImpl<>(pageContent, pageable, total); // <-- total count here is correct
    }

    public Page<UserDto> searchUsersByEmailAndMaxAmount(String email, double max, int page, int size) {
        // Create Pageable
        Pageable pageable = PageRequest.of(page, size);

        // Call repository with both arguments
        Page<User> usersPage = userRepository.findByEmailContainingIgnoreCase(email, pageable);

        // Filter users by lastAmount
        List<UserDto> filtered = usersPage.stream()
                .map(user -> {
                    Double lastAmount = eventStoreStorageEngine.getLastInduErrorAmount(user.getId());
                    if (lastAmount == null || lastAmount <= max) {
                        return new UserDto(user.getId(), user.getUsername(), user.getEmail(), lastAmount);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .toList();

        // Return new PageImpl with filtered content
        return new PageImpl<>(filtered, pageable, usersPage.getTotalElements());
    }


    public User syncFromKeycloakUser(UserRepresentation kcUser) {
        return userRepository.findById(kcUser.getId()).orElseGet(() -> {
            User user = User.builder()
                    .id(kcUser.getId())
                    .email(kcUser.getEmail())
                    .username(kcUser.getUsername())
                    .role(Role.USER)
                    .build();
            return userRepository.save(user);
        });
    }
}
