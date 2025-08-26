package cqrses.service;

import cqrses.dto.AdminUserDto;
import cqrses.entity.Role;
import cqrses.entity.User;
import cqrses.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserRoleService {

    private final Keycloak keycloakAdmin;
    private final UserRepository userRepository;

    private static final String REALM = "MainStageRealm";
    private static final String ADMIN_ROLE = "admin";
    private static final String USER_ROLE  = "user";

    @Transactional
    public void setRole(String userId, Role role) {
        RealmResource realm = keycloakAdmin.realm(REALM);
        UserResource userResource = realm.users().get(userId);

        RoleRepresentation adminRole = realm.roles().get(ADMIN_ROLE).toRepresentation();
        RoleRepresentation userRole = realm.roles().get(USER_ROLE).toRepresentation();

        // Remove old roles
        userResource.roles().realmLevel().remove(List.of(adminRole, userRole));

        // Add new role
        if (role == Role.ADMIN) {
            userResource.roles().realmLevel().add(List.of(adminRole));
        } else {
            userResource.roles().realmLevel().add(List.of(userRole));
        }

        // Optional: mirror in Mongo
        userRepository.findById(userId).ifPresent(u -> {
            u.setRole(role);
            userRepository.save(u);
        });
    }
    @Transactional
    public Role getRoleFromKeycloak(String userId) {
        RealmResource realm = keycloakAdmin.realm(REALM);
        UserResource userResource = realm.users().get(userId);

        List<RoleRepresentation> roles = userResource.roles().realmLevel().listAll();
        for (RoleRepresentation r : roles) {
            if (ADMIN_ROLE.equalsIgnoreCase(r.getName())) {
                return Role.ADMIN;
            }
        }
        return Role.USER;
    }
}
