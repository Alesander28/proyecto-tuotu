package pe.tuotu.sistema.seguridad.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Sesión abierta de un usuario. Se guarda solo el hash del token, nunca el token.
 * Al cerrar sesión o desactivar al usuario, la sesión se marca como revocada y
 * el token deja de valer de inmediato.
 */
@Entity
@Table(name = "sesion")
public class SesionToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sesion")
    private Long id;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    @Column(nullable = false)
    private boolean revocado = false;

    protected SesionToken() {
    }

    public SesionToken(String tokenHash, Usuario usuario) {
        this.tokenHash = tokenHash;
        this.usuario = usuario;
    }

    public Long getId() {
        return id;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public boolean isRevocado() {
        return revocado;
    }

    public void setRevocado(boolean revocado) {
        this.revocado = revocado;
    }
}
