package pe.tuotu.sistema.seguridad.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Ingrese su usuario.") String nombreUsuario,
        @NotBlank(message = "Ingrese su contraseña.") String contrasena) {
}
