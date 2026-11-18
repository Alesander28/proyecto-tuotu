package pe.tuotu.sistema.seguridad.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.tuotu.sistema.seguridad.dominio.Usuario;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByNombreUsuario(String nombreUsuario);

    boolean existsByNombreUsuario(String nombreUsuario);
}
