package com.udeci.reservas.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Datos necesarios para registrar un nuevo usuario")
public class RegisterRequest {

    @Schema(example = "eflecha", description = "Nombre de usuario único")
    private String username;

    @Schema(example = "12345", description = "Contraseña del usuario")
    private String password;

    @Schema(example = "ADMIN", description = "Rol asignado (ADMIN o USER)")
    private String role;

    @Schema(example = "eflecha@example.com", description = "Correo electrónico del usuario")
    private String email;

    // Getters y setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
