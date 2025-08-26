package cqrses.rest;

import cqrses.dto.AdminUserDto;
import cqrses.entity.Role;
import cqrses.service.InduErrorService;
import cqrses.service.UserRoleService;
import cqrses.service.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@CrossOrigin("*")
public class AdminUserController {

    private final Keycloak keycloakAdmin;
    private final UserService userService;
    private final InduErrorService induErrorService;
    private final UserRoleService userRoleService;
    private static final String REALM = "MainStageRealm";

    // 1. 🔍 List all Keycloak users
    @PreAuthorize("hasRole('admin')")
    @GetMapping("/keycloak")
    public ResponseEntity<List<UserRepresentation>> getAllKeycloakUsers() {
        List<UserRepresentation> users = keycloakAdmin
                .realm(REALM)
                .users()
                .list();
        return ResponseEntity.ok(users);
    }
    @PreAuthorize("hasRole('admin')")
    @PostMapping("/validate/{userId}")
    public ResponseEntity<Map<String, String>> validateAndInitializeUser(@PathVariable String userId) {
        UserRepresentation kcUser = keycloakAdmin
                .realm(REALM)
                .users()
                .get(userId)
                .toRepresentation();

        userService.syncFromKeycloakUser(kcUser);
        induErrorService.initializeUser(userId);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "✅ User validated and initialized: " + userId
        ));
    }
    @PreAuthorize("hasRole('admin')")
    @GetMapping("/not-validated")
    public List<UserRepresentation> getNonValidatedUsers() {
        List<UserRepresentation> allKeycloakUsers = keycloakAdmin.realm("MainStageRealm").users().list();

        return allKeycloakUsers.stream()
                .filter(kcUser -> {
                    boolean existsInMongo = userService.existsById(kcUser.getId());
                    boolean hasStream = induErrorService.streamExistsForUser(kcUser.getId());
                    return !existsInMongo || !hasStream;
                })
                .toList();
    }
    @PreAuthorize("hasRole('admin')")
    @GetMapping("/all-keycloak")
    public List<AdminUserDto> getAllKeycloakUsersWithValidation() {
        List<UserRepresentation> allKeycloakUsers = keycloakAdmin.realm(REALM).users().list();

        return allKeycloakUsers.stream().map(kcUser -> {
            boolean existsInMongo = userService.existsById(kcUser.getId());
            boolean hasStream = induErrorService.streamExistsForUser(kcUser.getId());
            boolean validated = existsInMongo && hasStream;

            // Get role from Keycloak
            Role role = userRoleService.getRoleFromKeycloak(kcUser.getId());

            return new AdminUserDto(
                    kcUser.getId(),
                    kcUser.getUsername(),
                    kcUser.getEmail(),
                    role,
                    validated
            );
        }).toList();
    }



    @Data
    static class RoleChangeRequest {
        private String role; // "admin" or "user"
    }

    @PreAuthorize("hasRole('admin')")
    @PostMapping("/{userId}/role")
    public Map<String, String> changeUserRole(
            @PathVariable String userId,
            @RequestBody RoleChangeRequest request
    ) {
        Role newRole = "admin".equalsIgnoreCase(request.getRole()) ? Role.ADMIN : Role.USER;
        userRoleService.setRole(userId, newRole);

        return Map.of(
                "status", "success",
                "message", "Role changed to " + newRole + " for user " + userId
        );
    }

}
