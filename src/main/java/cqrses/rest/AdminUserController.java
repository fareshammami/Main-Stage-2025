package cqrses.rest;

import cqrses.service.InduErrorService;
import cqrses.service.UserService;
import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.Keycloak;
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
}
