package pe.tuotu.sistema.cajas.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AbrirCajaRequest(
        @NotNull(message = "Indique el monto inicial de la caja.")
        @DecimalMin(value = "0.00", message = "El monto inicial no puede ser negativo.")
        BigDecimal montoInicial) {
}
