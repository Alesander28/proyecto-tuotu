package pe.tuotu.sistema.seguridad.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
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
    @Transactional
    public void run(String... args) {
        // Permisos del Sprint 1: USUARIOS_GESTIONAR. Permisos del Sprint 2: PEDIDOS_*.
        // Los de inventario, ventas y reportes se usarán en los Sprints 3 y 4.
        // Módulo del gerente: AUDITORIA_VER, MESAS_GESTIONAR, CAJAS_VER. Operación de caja del cajero: CAJAS_OPERAR.
        Map<String, String> catalogo = Map.ofEntries(
                Map.entry("PEDIDOS_ESTADO", "Cambiar el estado de los pedidos"),
                Map.entry("USUARIOS_GESTIONAR", "Registrar, editar y desactivar usuarios"),
                Map.entry("PEDIDOS_REGISTRAR", "Registrar pedidos"),
                Map.entry("PEDIDOS_VER", "Ver el estado de los pedidos"),
                Map.entry("INVENTARIO_GESTIONAR", "Gestionar productos e inventario"),
                Map.entry("VENTAS_REGISTRAR", "Registrar ventas y emitir comprobantes"),
                Map.entry("REPORTES_VER", "Ver reportes"),
                Map.entry("AUDITORIA_VER", "Consultar la auditoría del sistema"),
                Map.entry("MESAS_GESTIONAR", "Administrar las mesas del restaurante"),
                Map.entry("CAJAS_VER", "Consultar las cajas y sus cierres"),
                Map.entry("CAJAS_OPERAR", "Abrir y cerrar la propia caja"));
        catalogo.forEach((codigo, descripcion) -> {
            if (permisos.findByCodigo(codigo).isEmpty()) {
                permisos.save(new Permiso(codigo, descripcion));
            }
        });

        crearRol("GERENTE", List.of("USUARIOS_GESTIONAR", "INVENTARIO_GESTIONAR", "REPORTES_VER", "PEDIDOS_VER", "AUDITORIA_VER",
                "MESAS_GESTIONAR", "CAJAS_VER"));
        crearRol("CAJERO", List.of("PEDIDOS_VER", "VENTAS_REGISTRAR", "CAJAS_OPERAR"));
        crearRol("MOZO", List.of("PEDIDOS_REGISTRAR", "PEDIDOS_VER", "PEDIDOS_ESTADO"));

        if (usuarios.findByNombreUsuario(adminUsuario).isEmpty()) {
            if (adminContrasena == null || adminContrasena.length() < 12 || adminContrasena.length() > 72) {
                throw new IllegalStateException("Configure ADMIN_CONTRASENA con una contraseña de 12 a 72 caracteres antes del primer inicio.");
            }
            Rol gerente = roles.findByNombre("GERENTE").orElseThrow();
            usuarios.save(new Usuario(adminUsuario, encoder.encode(adminContrasena), "Gerente del restaurante", gerente));
            LOG.warn("Se creó la cuenta inicial del gerente (usuario '{}'). Cambie su contraseña.", adminUsuario);
        }
    }

    private void crearRol(String nombre, List<String> codigos) {
        // La inicialización se ejecuta dentro de una única transacción. Así, los permisos
        // consultados y el rol existente pertenecen al mismo contexto de persistencia.
        Rol rol = roles.findByNombre(nombre).orElseGet(() -> new Rol(nombre, new HashSet<>()));

        for (String codigo : codigos) {
            boolean asignado = rol.getPermisos().stream()
                    .anyMatch(permiso -> permiso.getCodigo().equals(codigo));

            if (!asignado) {
                Permiso permiso = permisos.findByCodigo(codigo)
                        .orElseThrow(() -> new IllegalStateException(
                                "No existe el permiso requerido: " + codigo));
                rol.getPermisos().add(permiso);
            }
        }

        // También guarda los permisos nuevos en roles que ya existían en la base de datos.
        roles.save(rol);
    }
}
