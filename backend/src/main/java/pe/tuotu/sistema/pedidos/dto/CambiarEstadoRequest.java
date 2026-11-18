package pe.tuotu.sistema.pedidos.dto;

import jakarta.validation.constraints.NotNull;
import pe.tuotu.sistema.pedidos.dominio.EstadoPedido;

public record CambiarEstadoRequest(@NotNull(message = "Indique el nuevo estado.") EstadoPedido estado) {
}
