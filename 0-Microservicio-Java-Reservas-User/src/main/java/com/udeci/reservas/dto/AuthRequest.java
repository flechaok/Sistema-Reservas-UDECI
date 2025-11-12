package com.udeci.reservas.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Datos necesarios para registrarse o autenticarse")
public class AuthRequest {

    @Schema(description = "Nombre de usuario", example = "juanperez")
    private String username;

    @Schema(description = "Contraseña del usuario", example = "12345")
    private String password;

    @Schema(description = "Rol del usuario (opcional, por defecto 'USER')", example = "ADMIN")
    private String role;

    // los Getters y setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
