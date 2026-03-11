package org.service_b.workflow.security.controller;

import lombok.RequiredArgsConstructor;
import org.service_b.workflow.security.dto.UserResponse;
import org.service_b.workflow.security.entity.User;
import org.service_b.workflow.security.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;

    @GetMapping("/users")
    public List<UserResponse> listUsers() {
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<UserResponse> updateRole(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {

        String newRole = body.get("role");
        if (newRole == null || (!newRole.equals("ADMIN") && !newRole.equals("USER"))) {
            return ResponseEntity.badRequest().build();
        }

        return userRepository.findById(id)
                .map(user -> {
                    user.setRole(newRole);
                    userRepository.save(user);
                    return ResponseEntity.ok(toResponse(user));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .build();
    }
}
