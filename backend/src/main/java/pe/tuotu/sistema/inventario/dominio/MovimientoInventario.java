package pe.tuotu.sistema.inventario.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import pe.tuotu.sistema.seguridad.dominio.Usuario;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Entrada o salida de un insumo, con su cantidad, fecha, motivo y responsable. */
@Entity
@Table(name = "movimiento_inventario")
public class MovimientoInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_movimiento")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_insumo")
    private Insumo insumo;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoMovimiento tipo;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal cantidad;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora = LocalDateTime.now();

    @Column(length = 120)
    private String motivo;

    protected MovimientoInventario() {
    }

    public MovimientoInventario(Insumo insumo, Usuario usuario, TipoMovimiento tipo, BigDecimal cantidad, String motivo) {
        this.insumo = insumo;
        this.usuario = usuario;
        this.tipo = tipo;
        this.cantidad = cantidad;
        this.motivo = motivo;
    }

    public Long getId() {
        return id;
    }

    public Insumo getInsumo() {
        return insumo;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public TipoMovimiento getTipo() {
        return tipo;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public String getMotivo() {
        return motivo;
    }
}
