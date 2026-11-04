package pe.tuotu.sistema.pedidos.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.tuotu.sistema.pedidos.dominio.Producto;
import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    List<Producto> findByActivoTrueOrderByCategoriaIdAscNombreAsc();

    List<Producto> findAllByOrderByCategoriaIdAscNombreAsc();

    boolean existsByNombreIgnoreCaseAndCategoriaId(String nombre, Long categoriaId);
}
