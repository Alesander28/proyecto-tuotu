package pe.tuotu.sistema.inventario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InsumoCrearRequest(
        @NotBlank(message = "El nombre del insumo es obligatorio.")
        @Size(max = 80, message = "El nombre del insumo es demasiado largo.")
        String nombre,

        @NotBlank(message = "La unidad de medida es obligatoria.")
        @Size(max = 20, message = "La unidad de medida es demasiado larga.")
        String unidadMedida) {
}
