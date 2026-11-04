package pe.tuotu.sistema.pedidos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record PedidoRegistrarRequest(
        @NotBlank(message = "Indique la mesa.")
        @Size(max = 10, message = "La mesa es demasiado larga.")
        String mesa,

        @NotEmpty(message = "El pedido debe tener al menos un producto.")
        List<@Valid ItemPedidoRequest> items) {
}
