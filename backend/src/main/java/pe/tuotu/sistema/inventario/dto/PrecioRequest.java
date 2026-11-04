package pe.tuotu.sistema.inventario.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PrecioRequest(
        @NotNull(message = "Indique el precio.")
        @DecimalMin(value = "0.01", message = "El precio debe ser mayor que cero.")
        BigDecimal precio) {
}
