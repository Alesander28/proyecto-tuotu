package pe.tuotu.sistema.cajas.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CerrarCajaRequest(
        @NotNull(message = "Indique el efectivo contado en la caja.")
        @DecimalMin(value = "0.00", message = "El efectivo contado no puede ser negativo.")
        BigDecimal montoContado,

        @Size(max = 200, message = "La observación es demasiado larga.")
        String observacion) {
}
