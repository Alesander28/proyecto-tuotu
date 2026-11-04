package pe.tuotu.sistema.pedidos.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ItemPedidoRequest(
        @NotNull(message = "Falta indicar el producto.") Long presentacionId,
        @Min(value = 1, message = "La cantidad debe ser al menos 1.") int cantidad,
        boolean paraLlevar,
        boolean extraQueso) {
}
