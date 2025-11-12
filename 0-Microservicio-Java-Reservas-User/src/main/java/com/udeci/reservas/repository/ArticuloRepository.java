//package com.udeci.reservas.repository;
//
//import com.udeci.reservas.model.Articulo;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.stereotype.Repository;
//
//@Repository
//public interface ArticuloRepository extends JpaRepository<Articulo, Long> {
//}
package com.udeci.reservas.repository;

import com.udeci.reservas.model.Articulo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ArticuloRepository extends JpaRepository<Articulo, Long> {
    Optional<Articulo> findByNombre(String nombre);
}
