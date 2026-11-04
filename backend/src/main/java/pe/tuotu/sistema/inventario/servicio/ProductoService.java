package pe.tuotu.sistema.inventario.servicio;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.tuotu.sistema.comun.ApiException;
import pe.tuotu.sistema.inventario.dto.CategoriaCrearRequest;
import pe.tuotu.sistema.inventario.dto.CategoriaDto;
import pe.tuotu.sistema.inventario.dto.PrecioRequest;
import pe.tuotu.sistema.inventario.dto.PresentacionRequest;
import pe.tuotu.sistema.inventario.dto.ProductoActualizarRequest;
import pe.tuotu.sistema.inventario.dto.ProductoAdminDto;
import pe.tuotu.sistema.inventario.dto.ProductoCrearRequest;
import pe.tuotu.sistema.pedidos.dominio.Categoria;
import pe.tuotu.sistema.pedidos.dominio.Presentacion;
import pe.tuotu.sistema.pedidos.dominio.Producto;
import pe.tuotu.sistema.pedidos.dto.PresentacionDto;
import pe.tuotu.sistema.pedidos.repositorio.CategoriaRepository;
import pe.tuotu.sistema.pedidos.repositorio.PresentacionRepository;
import pe.tuotu.sistema.pedidos.repositorio.ProductoRepository;

import java.util.List;

/**
 * HU-010: el gerente administra la carta (categorías, productos y precios por presentación).
 * Un cambio de precio no altera los pedidos ya registrados, porque cada pedido guarda el precio con el que se vendió.
 */
@Service
public class ProductoService {

    private final CategoriaRepository categorias;
    private final ProductoRepository productos;
    private final PresentacionRepository presentaciones;

    public ProductoService(CategoriaRepository categorias, ProductoRepository productos,
                           PresentacionRepository presentaciones) {
        this.categorias = categorias;
        this.productos = productos;
        this.presentaciones = presentaciones;
    }

    @Transactional(readOnly = true)
    public List<CategoriaDto> listarCategorias() {
        return categorias.findAll(Sort.by("nombre")).stream().map(c -> new CategoriaDto(c.getId(), c.getNombre())).toList();
    }

    @Transactional
    public CategoriaDto crearCategoria(CategoriaCrearRequest solicitud) {
        String nombre = solicitud.nombre().trim();
        if (categorias.existsByNombreIgnoreCase(nombre)) {
            throw new ApiException(HttpStatus.CONFLICT, "La categoría «" + nombre + "» ya existe.");
        }
        Categoria categoria = categorias.save(new Categoria(nombre));
        return new CategoriaDto(categoria.getId(), categoria.getNombre());
    }

    /** Todos los productos, incluidos los desactivados (la carta para pedidos solo muestra los activos). */
    @Transactional(readOnly = true)
    public List<ProductoAdminDto> listar() {
        return productos.findAllByOrderByCategoriaIdAscNombreAsc().stream().map(this::aDto).toList();
    }

    @Transactional
    public ProductoAdminDto crear(ProductoCrearRequest solicitud) {
        Categoria categoria = buscarCategoria(solicitud.categoriaId());
        String nombre = solicitud.nombre().trim();
        if (productos.existsByNombreIgnoreCaseAndCategoriaId(nombre, categoria.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe «" + nombre + "» en la categoría " + categoria.getNombre() + ".");
        }
        Producto producto = productos.save(new Producto(nombre, categoria));
        for (PresentacionRequest p : solicitud.presentaciones()) {
            presentaciones.save(new Presentacion(p.nombre().trim(), p.precio(), producto));
        }
        return aDto(producto);
    }

    @Transactional
    public ProductoAdminDto actualizar(Long id, ProductoActualizarRequest solicitud) {
        Producto producto = buscarProducto(id);
        producto.setNombre(solicitud.nombre().trim());
        producto.setCategoria(buscarCategoria(solicitud.categoriaId()));
        producto.setActivo(solicitud.activo());
        return aDto(productos.save(producto));
    }

    @Transactional
    public ProductoAdminDto agregarPresentacion(Long idProducto, PresentacionRequest solicitud) {
        Producto producto = buscarProducto(idProducto);
        presentaciones.save(new Presentacion(solicitud.nombre().trim(), solicitud.precio(), producto));
        return aDto(producto);
    }

    @Transactional
    public ProductoAdminDto actualizarPrecio(Long idPresentacion, PrecioRequest solicitud) {
        Presentacion presentacion = presentaciones.findById(idPresentacion)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "La presentación no existe."));
        presentacion.setPrecio(solicitud.precio());
        presentaciones.save(presentacion);
        return aDto(presentacion.getProducto());
    }

    private ProductoAdminDto aDto(Producto p) {
        List<PresentacionDto> lista = presentaciones.findByProductoIdOrderByPrecioAsc(p.getId()).stream()
                .map(x -> new PresentacionDto(x.getId(), x.getNombre(), x.getPrecio()))
                .toList();
        return new ProductoAdminDto(p.getId(), p.getNombre(), p.isActivo(), p.getCategoria().getId(),
                p.getCategoria().getNombre(), lista);
    }

    private Producto buscarProducto(Long id) {
        return productos.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "El producto no existe."));
    }

    private Categoria buscarCategoria(Long id) {
        return categorias.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "La categoría elegida no existe."));
    }
}
