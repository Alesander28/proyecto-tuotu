package pe.tuotu.sistema.inventario.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.tuotu.sistema.inventario.dominio.MovimientoInventario;

import java.time.LocalDateTime;
import java.util.List;

public interface MovimientoRepository extends JpaRepository<MovimientoInventario, Long> {

    List<MovimientoInventario> findTop50ByInsumoIdOrderByFechaHoraDesc(Long idInsumo);

    List<MovimientoInventario> findByFechaHoraGreaterThanEqualAndFechaHoraLessThan(LocalDateTime desde, LocalDateTime hasta);
}
