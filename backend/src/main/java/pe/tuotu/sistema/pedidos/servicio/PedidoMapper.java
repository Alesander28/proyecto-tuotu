package pe.tuotu.sistema.pedidos.servicio;

import pe.tuotu.sistema.pedidos.dominio.DetallePedido;
import pe.tuotu.sistema.pedidos.dominio.Pedido;
import pe.tuotu.sistema.pedidos.dto.DetallePedidoDto;
import pe.tuotu.sistema.pedidos.dto.PedidoDto;

public final class PedidoMapper {

    private PedidoMapper() {
    }

    public static PedidoDto aDto(Pedido p) {
        return new PedidoDto(p.getId(), p.getCodigo(), p.getMesa(), p.getFechaHora(), p.getEstado().name(),
                p.getMozo().getNombreCompleto(), p.getDetalles().stream().map(PedidoMapper::aDto).toList(),
                p.calcularTotal());
    }

    private static DetallePedidoDto aDto(DetallePedido d) {
        return new DetallePedidoDto(d.getPresentacion().getProducto().getNombre(), d.getPresentacion().getNombre(),
                d.getCantidad(), d.getPrecioUnitario(), d.isParaLlevar(), d.isExtraQueso(), d.calcularSubtotal());
    }
}
