package pe.tuotu.sistema.inventario.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PresentacionRequest(
        @NotBlank(message = "El nombre de la presentación es obligatorio.")
        @Size(max = 40, message = "El nombre de la presentación es demasiado largo.")
        String nombre,

        @NotNull(message = "Indique el precio.")
        @DecimalMin(value = "0.01", message = "El precio debe ser mayor que cero.")
        BigDecimal precio) {
}
