package pe.tuotu.sistema.auditoria.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** Constancia de una operación del sistema. Es de solo lectura: no tiene métodos para modificarla. */
@Entity
@Table(name = "auditoria")
public class RegistroAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_auditoria")
    private Long id;

    @Column(name = "fecha_hora", nullable = false, updatable = false)
    private LocalDateTime fechaHora = LocalDateTime.now();

    @Column(nullable = false, updatable = false, length = 40)
    private String usuario;

    @Column(nullable = false, updatable = false, length = 30)
    private String modulo;

    @Column(nullable = false, updatable = false, length = 40)
    private String tipo;

    @Column(nullable = false, updatable = false, length = 300)
    private String descripcion;

    @Column(updatable = false, length = 40)
    private String referencia;

    protected RegistroAuditoria() {
    }

    public RegistroAuditoria(String usuario, String modulo, String tipo, String descripcion, String referencia) {
        this.usuario = recortar(usuario == null ? "sistema" : usuario, 40);
        this.modulo = recortar(modulo, 30);
        this.tipo = recortar(tipo, 40);
        this.descripcion = recortar(descripcion, 300);
        this.referencia = referencia == null ? null : recortar(referencia, 40);
    }

    private static String recortar(String texto, int maximo) {
        return texto.length() <= maximo ? texto : texto.substring(0, maximo);
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getModulo() {
        return modulo;
    }

    public String getTipo() {
        return tipo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getReferencia() {
        return referencia;
    }
}
