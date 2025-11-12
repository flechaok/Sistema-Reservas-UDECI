package com.udeci.reservas.config;

import com.udeci.reservas.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class InitialPasswordEncoder {

    /**
     * Este método se ejecuta automáticamente al iniciar la app.
     * Revisa todos los usuarios y codifica las contraseñas planas con BCrypt.
     */
    @Bean
    CommandLineRunner encodePasswords(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            userRepository.findAll().forEach(user -> {
                String rawPassword = user.getPassword();
                // Solo codifica si la contraseña no está ya en formato BCrypt
                if (!rawPassword.startsWith("$2a$")) {
                    user.setPassword(passwordEncoder.encode(rawPassword));
                    userRepository.save(user);
                    System.out.println("✅ Contraseña codificada para usuario: " + user.getUsername());
                }
            });
        };
    }
}
