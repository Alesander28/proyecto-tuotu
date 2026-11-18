package pe.tuotu.sistema.cajas.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.tuotu.sistema.cajas.dominio.Caja;
import pe.tuotu.sistema.cajas.dominio.EstadoCaja;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CajaRepository extends JpaRepository<Caja, Long> {

    Optional<Caja> findFirstByCajeroIdAndEstado(Long idCajero, EstadoCaja estado);

    long countByEstado(EstadoCaja estado);

    List<Caja> findByFechaAperturaGreaterThanEqualAndFechaAperturaLessThanOrderByFechaAperturaDesc(LocalDateTime desde, LocalDateTime hasta);
}
