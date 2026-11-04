package pe.tuotu.sistema.pedidos.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.tuotu.sistema.pedidos.dominio.EstadoPedido;
import pe.tuotu.sistema.pedidos.dominio.Pedido;
import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    List<Pedido> findTop100ByOrderByFechaHoraDesc();

    List<Pedido> findTop100ByEstadoOrderByFechaHoraDesc(EstadoPedido estado);
}
