package pe.tuotu.sistema.inventario.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/** Ingrediente o material del almacén, con su stock actual. */
@Entity
@Table(name = "insumo")
public class Insumo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_insumo")
    private Long id;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(name = "unidad_medida", nullable = false, length = 20)
    private String unidadMedida;

    @Column(name = "stock_actual", nullable = false, precision = 10, scale = 2)
    private BigDecimal stockActual = BigDecimal.ZERO;

    protected Insumo() {
    }

    public Insumo(String nombre, String unidadMedida, BigDecimal stockInicial) {
        this.nombre = nombre;
        this.unidadMedida = unidadMedida;
        this.stockActual = stockInicial;
    }

    public void aumentarStock(BigDecimal cantidad) {
        stockActual = stockActual.add(cantidad);
    }

    /** El stock nunca puede quedar en negativo (RN-12). */
    public void disminuirStock(BigDecimal cantidad) {
        if (stockActual.compareTo(cantidad) < 0) {
            throw new IllegalArgumentException("Stock insuficiente");
        }
        stockActual = stockActual.subtract(cantidad);
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getUnidadMedida() {
        return unidadMedida;
    }

    public BigDecimal getStockActual() {
        return stockActual;
    }
}
