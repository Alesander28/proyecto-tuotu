package pe.tuotu.sistema.inventario.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import pe.tuotu.sistema.inventario.dominio.Insumo;
import pe.tuotu.sistema.inventario.repositorio.InsumoRepository;

import java.math.BigDecimal;

/** Carga insumos DE EJEMPLO para poder probar el inventario. Reemplácelos con los reales del restaurante. */
@Component
@Order(3)
public class InsumosIniciales implements CommandLineRunner {

    private static final Logger LOG = LoggerFactory.getLogger(InsumosIniciales.class);

    private final InsumoRepository insumos;

    public InsumosIniciales(InsumoRepository insumos) {
        this.insumos = insumos;
    }

    @Override
    public void run(String... args) {
        if (insumos.count() > 0) {
            return;
        }
        insumos.save(new Insumo("Queso mozzarella", "kg", new BigDecimal("12.50")));
        insumos.save(new Insumo("Pollo", "kg", new BigDecimal("30.00")));
        insumos.save(new Insumo("Champiñones", "kg", new BigDecimal("4.00")));
        insumos.save(new Insumo("Piña", "kg", new BigDecimal("6.50")));
        insumos.save(new Insumo("Salsa BBQ", "litros", new BigDecimal("8.00")));
        LOG.info("Se cargaron {} insumos de ejemplo.", insumos.count());
    }
}
