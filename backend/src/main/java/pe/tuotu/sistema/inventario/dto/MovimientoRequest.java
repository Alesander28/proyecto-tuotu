package pe.tuotu.sistema.inventario.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** El motivo es opcional en una entrada y obligatorio en una salida (consumo, merma, etc.). */
public record MovimientoRequest(
        @NotNull(message = "Indique la cantidad.")
        @DecimalMin(value = "0.01", message = "La cantidad debe ser mayor que cero.")
        BigDecimal cantidad,

        @Size(max = 120, message = "El motivo es demasiado largo.")
        String motivo) {
}
