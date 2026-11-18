package pe.tuotu.sistema.inventario.controlador;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.tuotu.sistema.inventario.dto.CategoriaCrearRequest;
import pe.tuotu.sistema.inventario.dto.CategoriaDto;
import pe.tuotu.sistema.inventario.dto.PrecioRequest;
import pe.tuotu.sistema.inventario.dto.PresentacionRequest;
import pe.tuotu.sistema.inventario.dto.ProductoActualizarRequest;
import pe.tuotu.sistema.inventario.dto.ProductoAdminDto;
import pe.tuotu.sistema.inventario.dto.ProductoCrearRequest;
import pe.tuotu.sistema.inventario.servicio.ProductoService;

import java.util.List;

/** Administración de la carta. Solo accesible con el permiso INVENTARIO_GESTIONAR (gerente). */
@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping
    public List<ProductoAdminDto> listar() {
        return productoService.listar();
    }

    @PostMapping
    public ResponseEntity<ProductoAdminDto> crear(@Valid @RequestBody ProductoCrearRequest solicitud,
                                                  Authentication autenticacion) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productoService.crear(solicitud, autenticacion.getName()));
    }

    @PutMapping("/{id}")
    public ProductoAdminDto actualizar(@PathVariable Long id, @Valid @RequestBody ProductoActualizarRequest solicitud,
                                       Authentication autenticacion) {
        return productoService.actualizar(id, solicitud, autenticacion.getName());
    }

    @PostMapping("/{id}/presentaciones")
    public ProductoAdminDto agregarPresentacion(@PathVariable Long id, @Valid @RequestBody PresentacionRequest solicitud,
                                                Authentication autenticacion) {
        return productoService.agregarPresentacion(id, solicitud, autenticacion.getName());
    }

    @PutMapping("/presentaciones/{id}/precio")
    public ProductoAdminDto actualizarPrecio(@PathVariable Long id, @Valid @RequestBody PrecioRequest solicitud,
                                             Authentication autenticacion) {
        return productoService.actualizarPrecio(id, solicitud, autenticacion.getName());
    }

    @GetMapping("/categorias")
    public List<CategoriaDto> categorias() {
        return productoService.listarCategorias();
    }

    @PostMapping("/categorias")
    public ResponseEntity<CategoriaDto> crearCategoria(@Valid @RequestBody CategoriaCrearRequest solicitud,
                                                       Authentication autenticacion) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productoService.crearCategoria(solicitud, autenticacion.getName()));
    }
}
