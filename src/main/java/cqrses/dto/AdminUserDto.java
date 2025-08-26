package cqrses.dto;

import cqrses.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminUserDto {
    private String id;
    private String username;
    private String email;
    private Role role;      // Role from Keycloak
    private boolean validated; // true if user exists in Mongo and has stream
}
