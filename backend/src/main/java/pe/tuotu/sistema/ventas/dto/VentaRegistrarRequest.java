package pe.tuotu.sistema.ventas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VentaRegistrarRequest(
        @NotNull(message = "Elija el pedido a cobrar.") Long pedidoId,
        @NotBlank(message = "Elija el método de pago.") String metodoPago) {
}
