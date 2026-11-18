package pe.tuotu.sistema.seguridad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Ingrese su usuario.")
        @Size(max = 40, message = "El nombre de usuario es demasiado largo.") String nombreUsuario,
        @NotBlank(message = "Ingrese su contraseña.")
        @Size(max = 72, message = "La contraseña es demasiado larga.") String contrasena) {
}
