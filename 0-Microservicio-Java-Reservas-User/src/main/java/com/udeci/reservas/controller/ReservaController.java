package com.udeci.reservas.controller;

import com.udeci.reservas.model.*;
import com.udeci.reservas.repository.*;

//import io.swagger.v3.oas.annotations.media.Content;
//import io.swagger.v3.oas.annotations.media.Schema;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/reservas")
@CrossOrigin(origins = "*")
public class ReservaController {

    private final ReservaRepository reservaRepository;
    private final PersonaRepository personaRepository;
    private final SalaRepository salaRepository;
    private final ArticuloRepository articuloRepository;

    public ReservaController(ReservaRepository reservaRepository,
                             PersonaRepository personaRepository,
                             SalaRepository salaRepository,
                             ArticuloRepository articuloRepository) {
        this.reservaRepository = reservaRepository;
        this.personaRepository = personaRepository;
        this.salaRepository = salaRepository;
        this.articuloRepository = articuloRepository;
    }

    // ---- GET all
    @GetMapping
    public List<Reserva> getAll() {
        return reservaRepository.findAll();
    }

    // ---- GET by id (necesario para editar desde Django)
    @GetMapping("/{id}")
    public ResponseEntity<Reserva> getById(@PathVariable Long id) {
        return reservaRepository.findById(id)
                .map(r -> ResponseEntity.ok(r))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

   // ---- POST crear (acepta ids o texto)
@PostMapping
@io.swagger.v3.oas.annotations.Operation(
  summary = "Crear reserva",
  description = "Por cada entidad (persona/sala/artículo) podés enviar **nombre** o **id**. Si llegan ambos, se prioriza el **id**."
)
@io.swagger.v3.oas.annotations.parameters.RequestBody(
  required = true,
  content = @io.swagger.v3.oas.annotations.media.Content(
    mediaType = "application/json",
    schema = @io.swagger.v3.oas.annotations.media.Schema(
      implementation = com.udeci.reservas.dto.ReservaDoc.class
    ),
    examples = {
      @io.swagger.v3.oas.annotations.media.ExampleObject(
        name = "Por nombre",
        value = "{\n" +
                "  \"persona\": \"Emiliano Flecha\",\n" +
                "  \"sala\": \"Sala Norte\",\n" +
                "  \"articulo\": \"Proyector Epson EB-X05\",\n" +
                "  \"fechaHoraInicio\": \"2025-11-04T09:00:00\",\n" +
                "  \"fechaHoraFin\": \"2025-11-04T11:00:00\"\n" +
                "}"
      ),
      @io.swagger.v3.oas.annotations.media.ExampleObject(
        name = "Por IDs",
        value = "{\n" +
                "  \"id_persona\": 1,\n" +
                "  \"id_sala\": 2,\n" +
                "  \"id_articulo\": 3,\n" +
                "  \"fechaHoraInicio\": \"2025-11-04T09:00:00\",\n" +
                "  \"fechaHoraFin\": \"2025-11-04T11:00:00\"\n" +
                "}"
      )
    }
  )
)
public ResponseEntity<?> crearReserva(@RequestBody Map<String, Object> datos) {
    try {
        Persona persona = resolveOrCreatePersona(datos);
        Sala sala = resolveOrCreateSala(datos);
        Articulo articulo = resolveOrCreateArticulo(datos);

        Reserva reserva = new Reserva();
        reserva.setPersona(persona);
        reserva.setSala(sala);
        reserva.setArticulo(articulo);
        reserva.setFechaHoraInicio(LocalDateTime.parse(datos.get("fechaHoraInicio").toString()));
        reserva.setFechaHoraFin(LocalDateTime.parse(datos.get("fechaHoraFin").toString()));

        Reserva saved = reservaRepository.save(reserva);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    } catch (Exception e) {
        String msg = (e.getMessage() != null) ? e.getMessage() : e.toString();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", msg));
    }
}


// ---- PUT actualizar (acepta ids o texto)
@PutMapping("/{id}")
@io.swagger.v3.oas.annotations.Operation(
  summary = "Actualizar reserva",
  description = "Por cada entidad (persona/sala/artículo) podés enviar **nombre** o **id**. Si llegan ambos, se prioriza el **id**."
)
@io.swagger.v3.oas.annotations.parameters.RequestBody(
  required = true,
  content = @io.swagger.v3.oas.annotations.media.Content(
    mediaType = "application/json",
    schema = @io.swagger.v3.oas.annotations.media.Schema(
      implementation = com.udeci.reservas.dto.ReservaDoc.class
    ),
    examples = {
      @io.swagger.v3.oas.annotations.media.ExampleObject(
        name = "Por nombre",
        value = "{\n" +
                "  \"persona\": \"María López\",\n" +
                "  \"sala\": \"Sala Sur\",\n" +
                "  \"articulo\": \"Laptop HP EliteBook\",\n" +
                "  \"fechaHoraInicio\": \"2025-11-04T14:00:00\",\n" +
                "  \"fechaHoraFin\": \"2025-11-04T16:00:00\"\n" +
                "}"
      ),
      @io.swagger.v3.oas.annotations.media.ExampleObject(
        name = "Por IDs",
        value = "{\n" +
                "  \"id_persona\": 1,\n" +
                "  \"id_sala\": 2,\n" +
                "  \"id_articulo\": 3,\n" +
                "  \"fechaHoraInicio\": \"2025-11-04T14:00:00\",\n" +
                "  \"fechaHoraFin\": \"2025-11-04T16:00:00\"\n" +
                "}"
      )
    }
  )
)
public ResponseEntity<?> actualizarReserva(@PathVariable Long id, @RequestBody Map<String, Object> datos) {
    return reservaRepository.findById(id)
            .map(reserva -> {
                try {
                    Persona persona = resolveOrCreatePersona(datos);
                    Sala sala = resolveOrCreateSala(datos);
                    Articulo articulo = resolveOrCreateArticulo(datos);

                    reserva.setPersona(persona);
                    reserva.setSala(sala);
                    reserva.setArticulo(articulo);
                    reserva.setFechaHoraInicio(LocalDateTime.parse(datos.get("fechaHoraInicio").toString()));
                    reserva.setFechaHoraFin(LocalDateTime.parse(datos.get("fechaHoraFin").toString()));

                    Reserva updated = reservaRepository.save(reserva);
                    return ResponseEntity.ok(updated);
                } catch (Exception e) {
                    String msg = (e.getMessage() != null) ? e.getMessage() : e.toString();
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", msg));
                }
            })
            .orElseGet(() -> ResponseEntity.notFound().build());
}



    // ---- DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarReserva(@PathVariable Long id) {
        if (reservaRepository.existsById(id)) {
            reservaRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // -------------------------
    // Helpers: resolver o crear entidades por id o por nombre (texto)
    // -------------------------
    private Persona resolveOrCreatePersona(Map<String, Object> datos) {
        // si viene id_persona (Number) -> usar
        if (datos.containsKey("id_persona") && datos.get("id_persona") instanceof Number) {
            Long id = ((Number) datos.get("id_persona")).longValue();
            return personaRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Persona no encontrada con id: " + id));
        }
        // si viene texto "persona"
        if (datos.containsKey("persona") && datos.get("persona") != null) {
            String nombre = datos.get("persona").toString().trim();
            Optional<Persona> opt = personaRepository.findByNombre(nombre);
            if (opt.isPresent()) return opt.get();
            Persona p = new Persona();
            p.setNombre(nombre);
            return personaRepository.save(p);
        }
        throw new RuntimeException("Falta id_persona o persona");
    }

    private Sala resolveOrCreateSala(Map<String, Object> datos) {
        if (datos.containsKey("id_sala") && datos.get("id_sala") instanceof Number) {
            Long id = ((Number) datos.get("id_sala")).longValue();
            return salaRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Sala no encontrada con id: " + id));
        }
        if (datos.containsKey("sala") && datos.get("sala") != null) {
            String nombre = datos.get("sala").toString().trim();
            Optional<Sala> opt = salaRepository.findByNombre(nombre);
            if (opt.isPresent()) return opt.get();
            Sala s = new Sala();
            s.setNombre(nombre);
            return salaRepository.save(s);
        }
        throw new RuntimeException("Falta id_sala o sala");
    }

    private Articulo resolveOrCreateArticulo(Map<String, Object> datos) {
        if (datos.containsKey("id_articulo") && datos.get("id_articulo") instanceof Number) {
            Long id = ((Number) datos.get("id_articulo")).longValue();
            return articuloRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Artículo no encontrado con id: " + id));
        }
        if (datos.containsKey("articulo") && datos.get("articulo") != null) {
            String nombre = datos.get("articulo").toString().trim();
            Optional<Articulo> opt = articuloRepository.findByNombre(nombre);
            if (opt.isPresent()) return opt.get();
            Articulo a = new Articulo();
            a.setNombre(nombre);
            return articuloRepository.save(a);
        }
        throw new RuntimeException("Falta id_articulo o articulo");
    }
}




//package com.udeci.reservas.controller;
//
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//import com.udeci.reservas.model.*;
//import com.udeci.reservas.repository.*;
//
//import java.util.List;
//import java.util.Map;
//import java.time.LocalDateTime;
//
//import io.swagger.v3.oas.annotations.media.Content;
//import io.swagger.v3.oas.annotations.media.Schema;
//import io.swagger.v3.oas.annotations.media.ExampleObject;
//
//@RestController
//@RequestMapping("/reservas")
//@CrossOrigin(origins = "*") // permite consumo desde otro microservicio o front
//public class ReservaController {
//
//    private final ReservaRepository reservaRepository;
//    private final PersonaRepository personaRepository;
//    private final SalaRepository salaRepository;
//    private final ArticuloRepository articuloRepository;
//
//    public ReservaController(ReservaRepository reservaRepository,
//                             PersonaRepository personaRepository,
//                             SalaRepository salaRepository,
//                             ArticuloRepository articuloRepository) {
//        this.reservaRepository = reservaRepository;
//        this.personaRepository = personaRepository;
//        this.salaRepository = salaRepository;
//        this.articuloRepository = articuloRepository;
//    }
//
//    // 🔹 GET: listar todas las reservas
//    @GetMapping
//    public List<Reserva> getAll() {
//        return reservaRepository.findAll();
//    }
//
//    // 🔹 POST: crear reserva solo con IDs
//@PostMapping
//@io.swagger.v3.oas.annotations.parameters.RequestBody(
//    description = "Ejemplo de cuerpo para crear una reserva",
//    required = true,
//    content = @Content(
//        mediaType = "application/json",
//        schema = @Schema(example = "{\n" +
//                "  \"id_persona\": 1,\n" +
//                "  \"id_sala\": 1,\n" +
//                "  \"id_articulo\": 1,\n" +
//                "  \"fechaHoraInicio\": \"2025-10-19T10:00:00\",\n" +
//                "  \"fechaHoraFin\": \"2025-10-19T12:00:00\"\n" +
//                "}")
//    )
//)
//public ResponseEntity<?> crearReserva(@RequestBody Map<String, Object> datos) {
//    try {
//        Long idPersona = ((Number) datos.get("id_persona")).longValue();
//        Long idSala = ((Number) datos.get("id_sala")).longValue();
//        Long idArticulo = ((Number) datos.get("id_articulo")).longValue();
//
//        Persona persona = personaRepository.findById(idPersona)
//                .orElseThrow(() -> new RuntimeException("Persona no encontrada"));
//        Sala sala = salaRepository.findById(idSala)
//                .orElseThrow(() -> new RuntimeException("Sala no encontrada"));
//        Articulo articulo = articuloRepository.findById(idArticulo)
//                .orElseThrow(() -> new RuntimeException("Artículo no encontrado"));
//
//        Reserva reserva = new Reserva();
//        reserva.setPersona(persona);
//        reserva.setSala(sala);
//        reserva.setArticulo(articulo);
//        reserva.setFechaHoraInicio(LocalDateTime.parse(datos.get("fechaHoraInicio").toString()));
//        reserva.setFechaHoraFin(LocalDateTime.parse(datos.get("fechaHoraFin").toString()));
//
//        return ResponseEntity.ok(reservaRepository.save(reserva));
//
//    } catch (Exception e) {
//        return ResponseEntity
//                .status(HttpStatus.BAD_REQUEST)
//                .body(Map.of("error", e.getMessage()));
//    }
//}
//
//    // 🔹 PUT: actualizar reserva solo con IDs
//    @PutMapping("/{id}")
//    @io.swagger.v3.oas.annotations.parameters.RequestBody(
//        description = "Cuerpo del request para actualizar una reserva existente",
//        required = true,
//        content = @Content(
//            mediaType = "application/json",
//            schema = @Schema(implementation = Object.class),
//            examples = {
//                @ExampleObject(
//                    name = "Ejemplo de actualización de reserva",
//                    value = "{\n" +
//                            "  \"id_persona\": 1,\n" +
//                            "  \"id_sala\": 2,\n" +
//                            "  \"id_articulo\": 3,\n" +
//                            "  \"fechaHoraInicio\": \"2025-10-14T09:00:00\",\n" +
//                            "  \"fechaHoraFin\": \"2025-10-14T11:00:00\"\n" +
//                            "}"
//                )
//            }
//        )
//    )
//    public Reserva actualizarReserva(@PathVariable Long id, @RequestBody Map<String, Object> datos) {
//        return reservaRepository.findById(id)
//            .map(reserva -> {
//                Long idPersona = ((Number) datos.get("id_persona")).longValue();
//                Long idSala = ((Number) datos.get("id_sala")).longValue();
//                Long idArticulo = ((Number) datos.get("id_articulo")).longValue();
//
//                reserva.setPersona(personaRepository.findById(idPersona).orElse(null));
//                reserva.setSala(salaRepository.findById(idSala).orElse(null));
//                reserva.setArticulo(articuloRepository.findById(idArticulo).orElse(null));
//
//                // 🔸 Corrección aquí también
//                reserva.setFechaHoraInicio(LocalDateTime.parse(datos.get("fechaHoraInicio").toString()));
//                reserva.setFechaHoraFin(LocalDateTime.parse(datos.get("fechaHoraFin").toString()));
//
//                return reservaRepository.save(reserva);
//            })
//            .orElseThrow(() -> new RuntimeException("Reserva no encontrada con id: " + id));
//    }
//
//    // 🔹 DELETE: eliminar reserva
//    @DeleteMapping("/{id}")
//    public void eliminarReserva(@PathVariable Long id) {
//        reservaRepository.deleteById(id);
//    }
//}
