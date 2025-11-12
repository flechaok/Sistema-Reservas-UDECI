package com.udeci.reservas.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ReservaDoc", description = "Body aceptado: usar nombres o IDs por entidad")
public class ReservaDoc {

  // Opción por nombre (cualquiera de estos 3 puede usarse en lugar de los IDs)
  @Schema(example = "Emiliano Flecha", description = "Alternativa a id_persona")
  public String persona;

  @Schema(example = "Sala Norte", description = "Alternativa a id_sala")
  public String sala;

  @Schema(example = "Proyector Epson EB-X05", description = "Alternativa a id_articulo")
  public String articulo;

  // Opción por ID (cualquiera de estos 3 puede usarse en lugar de los nombres)
  @Schema(example = "1", description = "Alternativa a persona")
  public Long id_persona;

  @Schema(example = "2", description = "Alternativa a sala")
  public Long id_sala;

  @Schema(example = "3", description = "Alternativa a articulo")
  public Long id_articulo;

  // Siempre requeridos:
  @Schema(example = "2025-11-04T09:00:00", description = "Obligatorio (ISO-8601)")
  public String fechaHoraInicio;

  @Schema(example = "2025-11-04T11:00:00", description = "Obligatorio (ISO-8601)")
  public String fechaHoraFin;
}
