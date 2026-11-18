package pe.tuotu.sistema.pedidos.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.tuotu.sistema.pedidos.dominio.Presentacion;
import java.util.List;

public interface PresentacionRepository extends JpaRepository<Presentacion, Long> {

    List<Presentacion> findAllByOrderByPrecioAsc();

    List<Presentacion> findByProductoIdOrderByPrecioAsc(Long idProducto);
}
