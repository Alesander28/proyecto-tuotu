package pe.tuotu.sistema.seguridad.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;

/** Perfil del personal (GERENTE, CAJERO o MOZO) con sus permisos. */
@Entity
@Table(name = "rol")
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rol")
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String nombre;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "rol_permiso",
            joinColumns = @JoinColumn(name = "id_rol"),
            inverseJoinColumns = @JoinColumn(name = "id_permiso"))
    private Set<Permiso> permisos = new HashSet<>();

    protected Rol() {
    }

    public Rol(String nombre, Set<Permiso> permisos) {
        this.nombre = nombre;
        this.permisos = permisos;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public Set<Permiso> getPermisos() {
        return permisos;
    }
}
