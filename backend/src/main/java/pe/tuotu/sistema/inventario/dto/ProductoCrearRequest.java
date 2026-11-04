package pe.tuotu.sistema.inventario.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ProductoCrearRequest(
        @NotBlank(message = "El nombre del producto es obligatorio.")
        @Size(max = 80, message = "El nombre del producto es demasiado largo.")
        String nombre,

        @NotNull(message = "Elija una categoría.")
        Long categoriaId,

        @NotEmpty(message = "El producto debe tener al menos una presentación con su precio.")
        List<@Valid PresentacionRequest> presentaciones) {
}
