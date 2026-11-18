package pe.tuotu.sistema.inventario.repositorio;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.tuotu.sistema.inventario.dominio.Insumo;

import java.util.List;
import java.util.Optional;

public interface InsumoRepository extends JpaRepository<Insumo, Long> {

    List<Insumo> findByNombreContainingIgnoreCaseOrderByNombreAsc(String texto);

    boolean existsByNombreIgnoreCase(String nombre);

    /** Bloquea la fila mientras se actualiza el stock, para que dos movimientos simultáneos no se pisen. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Insumo i where i.id = :id")
    Optional<Insumo> buscarParaActualizar(@Param("id") Long id);
}
