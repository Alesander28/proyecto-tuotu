package pe.tuotu.sistema.ventas.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.tuotu.sistema.pedidos.dominio.EstadoPedido;
import pe.tuotu.sistema.pedidos.dominio.Pedido;
import pe.tuotu.sistema.ventas.dominio.Venta;

import java.time.LocalDateTime;
import java.util.List;

public interface VentaRepository extends JpaRepository<Venta, Long> {

    boolean existsByPedidoId(Long idPedido);

    List<Venta> findTop100ByOrderByFechaHoraDesc();

    List<Venta> findByFechaHoraGreaterThanEqualAndFechaHoraLessThan(LocalDateTime desde, LocalDateTime hasta);

    /** Pedidos en el estado indicado que todavía no tienen venta (pendientes de cobro). */
    @Query("select p from Pedido p where p.estado = :estado and not exists "
            + "(select v.id from Venta v where v.pedido = p) order by p.fechaHora asc")
    List<Pedido> pedidosSinVenta(@Param("estado") EstadoPedido estado);
}
