package pe.tuotu.sistema.pedidos.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import pe.tuotu.sistema.pedidos.dominio.Categoria;
import pe.tuotu.sistema.pedidos.dominio.Presentacion;
import pe.tuotu.sistema.pedidos.dominio.Producto;
import pe.tuotu.sistema.pedidos.repositorio.CategoriaRepository;
import pe.tuotu.sistema.pedidos.repositorio.PresentacionRepository;
import pe.tuotu.sistema.pedidos.repositorio.ProductoRepository;
import java.math.BigDecimal;
import java.util.List;

/**
 * Carga una carta inicial para poder registrar pedidos en el Sprint 2. La administración completa de
 * productos (HU-010) se desarrolla en el Sprint 3. Los precios de las pizzas salen de la carta del restaurante;
 * los de las alitas y los tacos son DE EJEMPLO y deben reemplazarse con los reales.
 */
@Component
@Order(2)
public class CartaInicial implements CommandLineRunner {

    private static final Logger LOG = LoggerFactory.getLogger(CartaInicial.class);

    private final CategoriaRepository categorias;
    private final ProductoRepository productos;
    private final PresentacionRepository presentaciones;

    public CartaInicial(CategoriaRepository categorias, ProductoRepository productos,
                        PresentacionRepository presentaciones) {
        this.categorias = categorias;
        this.productos = productos;
        this.presentaciones = presentaciones;
    }

    @Override
    public void run(String... args) {
        if (categorias.count() > 0) {
            return;
        }
        Categoria clasicas = categorias.save(new Categoria("Pizzas clásicas"));
        for (String nombre : List.of("Tú o Tú", "Hawaiana", "Peperoni", "Tropical", "Americana", "Napolitana", "Vegetariana")) {
            pizza(clasicas, nombre, "22.00", "42.00");
        }

        Categoria especiales = categorias.save(new Categoria("Pizzas especiales"));
        for (String nombre : List.of("Frutaris", "Tingalesa", "Bolognesa", "Arcoíris", "Tres Quesos", "Al Pesto", "Texana", "Campesina")) {
            pizza(especiales, nombre, "27.00", "47.00");
        }
        for (String nombre : List.of("Escabechada", "Lomo Saltado", "Ají de Gallina", "Pechuga a la Huancaína")) {
            Producto p = productos.save(new Producto(nombre, especiales));
            presentaciones.save(new Presentacion("Familiar", new BigDecimal("47.00"), p));
        }

        // Precios de ejemplo (reemplazar con los reales en el Sprint 3)
        Categoria alitas = categorias.save(new Categoria("Alitas"));
        Producto bbq = productos.save(new Producto("Alitas BBQ (precio de ejemplo)", alitas));
        presentaciones.save(new Presentacion("6 unidades", new BigDecimal("18.00"), bbq));
        presentaciones.save(new Presentacion("12 unidades", new BigDecimal("34.00"), bbq));
        presentaciones.save(new Presentacion("24 unidades", new BigDecimal("64.00"), bbq));

        Categoria tacos = categorias.save(new Categoria("Tacos"));
        Producto mixto = productos.save(new Producto("Tacos mixtos (precio de ejemplo)", tacos));
        presentaciones.save(new Presentacion("Simple", new BigDecimal("12.00"), mixto));
        presentaciones.save(new Presentacion("Con queso", new BigDecimal("15.00"), mixto));

        LOG.info("Se cargó la carta inicial ({} productos).", productos.count());
    }

    private void pizza(Categoria categoria, String nombre, String personal, String familiar) {
        Producto producto = productos.save(new Producto(nombre, categoria));
        presentaciones.save(new Presentacion("Personal", new BigDecimal(personal), producto));
        presentaciones.save(new Presentacion("Familiar", new BigDecimal(familiar), producto));
    }
}
