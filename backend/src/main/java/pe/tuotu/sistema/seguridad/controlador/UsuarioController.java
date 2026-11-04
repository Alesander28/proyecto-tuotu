package pe.tuotu.sistema.seguridad.controlador;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.tuotu.sistema.seguridad.dto.UsuarioActualizarRequest;
import pe.tuotu.sistema.seguridad.dto.UsuarioCrearRequest;
import pe.tuotu.sistema.seguridad.dto.UsuarioDto;
import pe.tuotu.sistema.seguridad.servicio.UsuarioService;
import java.util.List;

/** Solo accesible con el permiso USUARIOS_GESTIONAR (gerente). */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public List<UsuarioDto> listar() {
        return usuarioService.listar();
    }

    @PostMapping
    public ResponseEntity<UsuarioDto> crear(@Valid @RequestBody UsuarioCrearRequest solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.crear(solicitud));
    }

    @PutMapping("/{id}")
    public UsuarioDto actualizar(@PathVariable Long id, @Valid @RequestBody UsuarioActualizarRequest solicitud) {
        return usuarioService.actualizar(id, solicitud);
    }

    @PatchMapping("/{id}/desactivar")
    public UsuarioDto desactivar(@PathVariable Long id, Authentication autenticacion) {
        return usuarioService.desactivar(id, autenticacion.getName());
    }
}
