package com.udeci.reservas.repository;

import com.udeci.reservas.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Para el LoginController (autenticación)
    Optional<User> findByUsername(String username);

    // Para el UserService (registro y búsquedas por email)
    Optional<User> findByEmail(String email);
}
