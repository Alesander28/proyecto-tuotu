package pe.tuotu.sistema.seguridad.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import pe.tuotu.sistema.seguridad.dominio.Permiso;
import pe.tuotu.sistema.seguridad.dominio.Rol;
import pe.tuotu.sistema.seguridad.dominio.Usuario;
import pe.tuotu.sistema.seguridad.repositorio.PermisoRepository;
import pe.tuotu.sistema.seguridad.repositorio.RolRepository;
import pe.tuotu.sistema.seguridad.repositorio.UsuarioRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Crea al iniciar los permisos, los tres roles y la cuenta inicial del gerente, para que
 * el sistema pueda usarse desde el primer arranque (el gerente es quien crea las demás cuentas).
 */
@Component
@Order(1)
public class DatosIniciales implements CommandLineRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DatosIniciales.class);

    private final PermisoRepository permisos;
    private final RolRepository roles;
    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final String adminUsuario;
    private final String adminContrasena;

    public DatosIniciales(PermisoRepository permisos, RolRepository roles, UsuarioRepository usuarios,
                          PasswordEncoder encoder,
                          @Value("${app.admin.usuario}") String adminUsuario,
                          @Value("${app.admin.contrasena}") String adminContrasena) {
        this.permisos = permisos;
        this.roles = roles;
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.adminUsuario = adminUsuario;
        this.adminContrasena = adminContrasena;
    }

    @Override
    public void run(String... args) {
        // Permisos del Sprint 1: USUARIOS_GESTIONAR. Permisos del Sprint 2: PEDIDOS_*.
        // Los de inventario, ventas y reportes se usarán en los Sprints 3 y 4.
        Map<String, String> catalogo = Map.of(
                "PEDIDOS_ESTADO", "Cambiar el estado de los pedidos",
                "USUARIOS_GESTIONAR", "Registrar, editar y desactivar usuarios",
                "PEDIDOS_REGISTRAR", "Registrar pedidos",
                "PEDIDOS_VER", "Ver el estado de los pedidos",
                "INVENTARIO_GESTIONAR", "Gestionar productos e inventario",
                "VENTAS_REGISTRAR", "Registrar ventas y emitir comprobantes",
                "REPORTES_VER", "Ver reportes");
        catalogo.forEach((codigo, descripcion) -> {
            if (permisos.findByCodigo(codigo).isEmpty()) {
                permisos.save(new Permiso(codigo, descripcion));
            }
        });

        crearRol("GERENTE", List.of("USUARIOS_GESTIONAR", "INVENTARIO_GESTIONAR", "REPORTES_VER", "PEDIDOS_VER", "PEDIDOS_ESTADO"));
        crearRol("CAJERO", List.of("PEDIDOS_VER", "VENTAS_REGISTRAR"));
        crearRol("MOZO", List.of("PEDIDOS_REGISTRAR", "PEDIDOS_VER", "PEDIDOS_ESTADO"));

        if (usuarios.findByNombreUsuario(adminUsuario).isEmpty()) {
            Rol gerente = roles.findByNombre("GERENTE").orElseThrow();
            usuarios.save(new Usuario(adminUsuario, encoder.encode(adminContrasena), "Gerente del restaurante", gerente));
            LOG.warn("Se creó la cuenta inicial del gerente (usuario '{}'). Cambie su contraseña.", adminUsuario);
        }
    }

    private void crearRol(String nombre, List<String> codigos) {
        Set<Permiso> conjunto = new HashSet<>();
        for (String codigo : codigos) {
            conjunto.add(permisos.findByCodigo(codigo).orElseThrow());
        }
        Rol existente = roles.findByNombre(nombre).orElse(null);
        if (existente == null) {
            roles.save(new Rol(nombre, conjunto));
        } else if (existente.getPermisos().addAll(conjunto)) {
            // Si el rol ya existía (base de datos de un Sprint anterior), se le agregan los permisos nuevos.
            roles.save(existente);
        }
    }
}
