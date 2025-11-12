package com.udeci.reservas.controller;

import com.udeci.reservas.model.Role;
import com.udeci.reservas.repository.UserRepository;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class UsersController {

    private final UserRepository userRepository;

    public UsersController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/users")
    public List<Map<String, Object>> listUsers() {
        return userRepository.findAll().stream()
                .map(u -> Map.of(
                        "id", u.getId(),
                        "username", u.getUsername(),
                        "roles", u.getRoles().stream().map(Role::getName).collect(Collectors.toList()),
                        "active", u.isActive()
                ))
                .collect(Collectors.toList());
    }
}