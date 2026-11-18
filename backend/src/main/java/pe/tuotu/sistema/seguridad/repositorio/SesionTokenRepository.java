package pe.tuotu.sistema.seguridad.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.tuotu.sistema.seguridad.dominio.SesionToken;
import java.util.Optional;
import java.time.LocalDateTime;

public interface SesionTokenRepository extends JpaRepository<SesionToken, Long> {

    Optional<SesionToken> findByTokenHashAndRevocadoFalseAndExpiraEnAfter(String tokenHash, LocalDateTime ahora);

    /** Revoca todas las sesiones abiertas de un usuario (por ejemplo, al desactivarlo). */
    @Modifying
    @Query("update SesionToken s set s.revocado = true where s.usuario.id = :idUsuario and s.revocado = false")
    int revocarPorUsuario(@Param("idUsuario") Long idUsuario);
}
