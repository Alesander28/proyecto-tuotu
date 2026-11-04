package pe.tuotu.sistema.seguridad.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.tuotu.sistema.seguridad.dominio.Rol;
import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Long> {

    Optional<Rol> findByNombre(String nombre);
}
