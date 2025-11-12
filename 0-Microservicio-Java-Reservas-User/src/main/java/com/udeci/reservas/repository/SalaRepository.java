//package com.udeci.reservas.repository;
//
//import com.udeci.reservas.model.Sala;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.stereotype.Repository;
//
//@Repository
//public interface SalaRepository extends JpaRepository<Sala, Long> {
//}
//
package com.udeci.reservas.repository;

import com.udeci.reservas.model.Sala;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SalaRepository extends JpaRepository<Sala, Long> {
    Optional<Sala> findByNombre(String nombre);
}

