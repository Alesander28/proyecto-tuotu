package pe.tuotu.sistema.inventario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoriaCrearRequest(
        @NotBlank(message = "El nombre de la categoría es obligatorio.")
        @Size(max = 50, message = "El nombre de la categoría es demasiado largo.")
        String nombre) {
}
