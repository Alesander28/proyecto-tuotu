package pe.tuotu.sistema.inventario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Un producto no se borra: se desactiva y deja de aparecer en la carta. */
public record ProductoActualizarRequest(
        @NotBlank(message = "El nombre del producto es obligatorio.")
        @Size(max = 80, message = "El nombre del producto es demasiado largo.")
        String nombre,

        @NotNull(message = "Elija una categoría.")
        Long categoriaId,

        boolean activo) {
}
