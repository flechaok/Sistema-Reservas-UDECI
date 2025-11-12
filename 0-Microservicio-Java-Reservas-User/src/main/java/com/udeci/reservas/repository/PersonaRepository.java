//package com.udeci.reservas.repository;
//
//import com.udeci.reservas.model.Persona;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.stereotype.Repository;
//
//@Repository
//public interface PersonaRepository extends JpaRepository<Persona, Long> {
//}

package com.udeci.reservas.repository;

import com.udeci.reservas.model.Persona;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PersonaRepository extends JpaRepository<Persona, Long> {
    Optional<Persona> findByNombre(String nombre);
}
