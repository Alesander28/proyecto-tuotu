package pe.tuotu.sistema.inventario.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record StockMinimoRequest(
        @NotNull(message = "Indique el stock mínimo.")
        @DecimalMin(value = "0.00", message = "El stock mínimo no puede ser negativo.")
        BigDecimal stockMinimo) {
}
