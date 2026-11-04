package pe.tuotu.sistema.seguridad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UsuarioCrearRequest(
        @NotBlank(message = "El nombre de usuario es obligatorio.")
        @Size(min = 3, max = 40, message = "El nombre de usuario debe tener entre 3 y 40 caracteres.")
        String nombreUsuario,

        @NotBlank(message = "La contraseña es obligatoria.")
        @Size(min = 6, max = 60, message = "La contraseña debe tener al menos 6 caracteres.")
        String contrasena,

        @NotBlank(message = "El nombre completo es obligatorio.")
        @Size(max = 100, message = "El nombre completo es demasiado largo.")
        String nombreCompleto,

        @NotNull(message = "Debe elegir un rol.")
        Long rolId) {
}
