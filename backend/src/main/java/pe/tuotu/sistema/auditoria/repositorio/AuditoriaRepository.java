package pe.tuotu.sistema.auditoria.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.tuotu.sistema.auditoria.dominio.RegistroAuditoria;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditoriaRepository extends JpaRepository<RegistroAuditoria, Long> {

    List<RegistroAuditoria> findByFechaHoraGreaterThanEqualAndFechaHoraLessThanOrderByFechaHoraDesc(LocalDateTime desde, LocalDateTime hasta);

    List<RegistroAuditoria> findTop10ByOrderByFechaHoraDesc();

    /** Historial de un pedido (cambios de estado y cobro), del más antiguo al más reciente. */
    List<RegistroAuditoria> findByReferenciaOrderByFechaHoraAsc(String referencia);
}
