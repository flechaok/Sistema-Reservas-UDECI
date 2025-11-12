package com.udeci.reservas.controller;

import com.udeci.reservas.dto.AuthRequest;
import com.udeci.reservas.dto.RegisterRequest;
import com.udeci.reservas.model.Role;
import com.udeci.reservas.model.User;
import com.udeci.reservas.repository.RoleRepository;
import com.udeci.reservas.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class LoginController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder; // usa el bean

    public LoginController(UserRepository userRepository,
                           RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder; // inyectado
    }

    // ===========================================
    //  Registro de usuario
    // ===========================================
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            String username = request.getUsername();
            String email = request.getEmail();
            String password = request.getPassword();
            String roleName = request.getRole() != null ? request.getRole() : "USER";

            if (username == null || email == null || password == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Debe ingresar username, email y password"));
            }

            if (userRepository.findByUsername(username).isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "El usuario ya existe"));
            }

            Role role = roleRepository.findByName(roleName);
            if (role == null) {
                role = new Role();
                role.setName(roleName);
                roleRepository.save(role);
            }

            User user = new User();
            user.setUsername(username);
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(password));
            user.setActive(true);
            user.setRoles(Set.of(role));

            userRepository.save(user);

            return ResponseEntity.ok(Map.of("message", "Usuario registrado correctamente", "user", username));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ===========================================
    //  Autenticación (login)
    // ===========================================
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        String username = request.getUsername();
        String password = request.getPassword();

        if (username == null || password == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Debe ingresar username y password"));
        }

        var userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuario no encontrado"));
        }

        var user = userOpt.get();
        if (!passwordEncoder.matches(password, user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Contraseña incorrecta"));
        }

        return ResponseEntity.ok(Map.of(
                "message", "Login exitoso",
                "username", user.getUsername(),
                "roles", user.getRoles().stream().map(Role::getName).toArray()
        ));
    }
}
