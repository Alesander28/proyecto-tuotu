package pe.tuotu.sistema.pedidos.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.tuotu.sistema.pedidos.dominio.EstadoPedido;
import pe.tuotu.sistema.pedidos.dominio.Pedido;
import java.time.LocalDateTime;
import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    List<Pedido> findTop100ByOrderByFechaHoraDesc();

    List<Pedido> findTop100ByEstadoOrderByFechaHoraDesc(EstadoPedido estado);

    List<Pedido> findByFechaHoraGreaterThanEqualAndFechaHoraLessThanOrderByFechaHoraDesc(LocalDateTime desde, LocalDateTime hasta);

    long countByFechaHoraGreaterThanEqualAndFechaHoraLessThan(LocalDateTime desde, LocalDateTime hasta);

    long countByEstado(EstadoPedido estado);

    List<Pedido> findTop50ByMesaOrderByFechaHoraDesc(String mesa);
}
