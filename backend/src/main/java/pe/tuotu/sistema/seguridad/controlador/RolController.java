package pe.tuotu.sistema.seguridad.controlador;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.tuotu.sistema.seguridad.dto.RolDto;
import pe.tuotu.sistema.seguridad.servicio.UsuarioService;
import java.util.List;

@RestController
@RequestMapping("/api/roles")
public class RolController {

    private final UsuarioService usuarioService;

    public RolController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public List<RolDto> listar() {
        return usuarioService.listarRoles();
    }
}
