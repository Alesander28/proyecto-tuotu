package pe.tuotu.sistema.seguridad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** La contraseña es opcional: si viene vacía, no se cambia. */
public record UsuarioActualizarRequest(
        @NotBlank(message = "El nombre completo es obligatorio.")
        @Size(max = 100, message = "El nombre completo es demasiado largo.")
        String nombreCompleto,

        @NotNull(message = "Debe elegir un rol.")
        Long rolId,

        String contrasena) {
}
