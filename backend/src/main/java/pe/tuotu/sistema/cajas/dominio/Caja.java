package pe.tuotu.sistema.cajas.dominio;

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

/** Turno de caja de un cajero: se abre con un monto inicial y se cierra con el arqueo del efectivo. */
@Entity
@Table(name = "caja")
public class Caja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_caja")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_usuario")
    private Usuario cajero;

    @Column(name = "fecha_apertura", nullable = false)
    private LocalDateTime fechaApertura = LocalDateTime.now();

    @Column(name = "monto_inicial", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoInicial;

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    @Column(name = "monto_contado", precision = 10, scale = 2)
    private BigDecimal montoContado;

    @Column(name = "efectivo_esperado", precision = 10, scale = 2)
    private BigDecimal efectivoEsperado;

    @Column(precision = 10, scale = 2)
    private BigDecimal diferencia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstadoCaja estado = EstadoCaja.ABIERTA;

    @Column(length = 200)
    private String observacion;

    protected Caja() {
    }

    public Caja(Usuario cajero, BigDecimal montoInicial) {
        this.cajero = cajero;
        this.montoInicial = montoInicial;
    }

    /** Cierra el turno guardando el arqueo: lo contado, lo esperado y la diferencia entre ambos. */
    public void cerrar(BigDecimal montoContado, BigDecimal efectivoEsperado, String observacion) {
        this.fechaCierre = LocalDateTime.now();
        this.montoContado = montoContado;
        this.efectivoEsperado = efectivoEsperado;
        this.diferencia = montoContado.subtract(efectivoEsperado);
        this.observacion = observacion;
        this.estado = EstadoCaja.CERRADA;
    }

    public Long getId() {
        return id;
    }

    public Usuario getCajero() {
        return cajero;
    }

    public LocalDateTime getFechaApertura() {
        return fechaApertura;
    }

    public BigDecimal getMontoInicial() {
        return montoInicial;
    }

    public LocalDateTime getFechaCierre() {
        return fechaCierre;
    }

    public BigDecimal getMontoContado() {
        return montoContado;
    }

    public BigDecimal getEfectivoEsperado() {
        return efectivoEsperado;
    }

    public BigDecimal getDiferencia() {
        return diferencia;
    }

    public EstadoCaja getEstado() {
        return estado;
    }

    public String getObservacion() {
        return observacion;
    }
}
