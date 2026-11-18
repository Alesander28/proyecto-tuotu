package pe.tuotu.sistema.pedidos.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.tuotu.sistema.pedidos.dominio.Categoria;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    boolean existsByNombreIgnoreCase(String nombre);
}
