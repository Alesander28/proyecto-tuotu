package pe.tuotu.sistema.inventario.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record InsumoCrearRequest(
        @NotBlank(message = "El nombre del insumo es obligatorio.")
        @Size(max = 80, message = "El nombre del insumo es demasiado largo.")
        String nombre,

        @NotBlank(message = "La unidad de medida es obligatoria.")
        @Size(max = 20, message = "La unidad de medida es demasiado larga.")
        String unidadMedida,

        /** Opcional: a partir de esta cantidad el sistema avisa que hay que reponer. */
        @DecimalMin(value = "0.00", message = "El stock mínimo no puede ser negativo.")
        BigDecimal stockMinimo) {
}
