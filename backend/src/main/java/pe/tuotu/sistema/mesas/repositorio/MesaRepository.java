package pe.tuotu.sistema.mesas.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.tuotu.sistema.mesas.dominio.Mesa;

import java.util.List;

public interface MesaRepository extends JpaRepository<Mesa, Long> {

    List<Mesa> findAllByOrderByNumeroAsc();

    boolean existsByNumeroIgnoreCase(String numero);
}
