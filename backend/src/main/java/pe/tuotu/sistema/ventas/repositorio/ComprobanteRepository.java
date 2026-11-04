package pe.tuotu.sistema.ventas.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.tuotu.sistema.ventas.dominio.Comprobante;

public interface ComprobanteRepository extends JpaRepository<Comprobante, Long> {
}
