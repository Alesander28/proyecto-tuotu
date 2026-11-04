package pe.tuotu.sistema.seguridad.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.tuotu.sistema.seguridad.dominio.Permiso;
import java.util.Optional;

public interface PermisoRepository extends JpaRepository<Permiso, Long> {

    Optional<Permiso> findByCodigo(String codigo);
}
