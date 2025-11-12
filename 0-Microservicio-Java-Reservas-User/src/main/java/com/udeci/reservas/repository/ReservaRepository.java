//package com.udeci.reservas.repository;
//
//import com.udeci.reservas.model.Reserva;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.stereotype.Repository;
//
//@Repository
//public interface ReservaRepository extends JpaRepository<Reserva, Long> {
//}
package com.udeci.reservas.repository;

import com.udeci.reservas.model.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {
}
