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

    /** Cantidad a partir de la cual el sistema avisa que hay que reponer el insumo (cero: sin aviso). */
    @Column(name = "stock_minimo", nullable = false, precision = 10, scale = 2, columnDefinition = "numeric(10,2) default 0")
    private BigDecimal stockMinimo = BigDecimal.ZERO;

    protected Insumo() {
    }

    public Insumo(String nombre, String unidadMedida, BigDecimal stockInicial) {
        this.nombre = nombre;
        this.unidadMedida = unidadMedida;
        this.stockActual = stockInicial;
    }

    public Insumo(String nombre, String unidadMedida, BigDecimal stockInicial, BigDecimal stockMinimo) {
        this(nombre, unidadMedida, stockInicial);
        this.stockMinimo = stockMinimo == null ? BigDecimal.ZERO : stockMinimo;
    }

    /** Agotado, bajo (en o por debajo del mínimo), próximo a agotarse (hasta 1.5 veces el mínimo) o normal. */
    public EstadoStock estadoStock() {
        if (stockActual.signum() <= 0) {
            return EstadoStock.AGOTADO;
        }
        BigDecimal minimo = getStockMinimo();
        if (minimo.signum() > 0) {
            if (stockActual.compareTo(minimo) <= 0) {
                return EstadoStock.BAJO;
            }
            if (stockActual.compareTo(minimo.multiply(new BigDecimal("1.5"))) <= 0) {
                return EstadoStock.PROXIMO;
            }
        }
        return EstadoStock.NORMAL;
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

    public BigDecimal getStockMinimo() {
        return stockMinimo == null ? BigDecimal.ZERO : stockMinimo;
    }

    public void setStockMinimo(BigDecimal stockMinimo) {
        this.stockMinimo = stockMinimo;
    }

    public BigDecimal getStockActual() {
        return stockActual;
    }
}
