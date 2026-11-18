import { useCallback, useEffect, useState } from 'react';
import { Download, Eye, Pencil, Pizza, Plus, Tag, Package, Layers3, CircleOff, LayoutGrid, List } from 'lucide-react';
import { api } from '../../comun/api.js';
import { exportarCsv } from '../../comun/exportar.js';
import { soles } from '../../comun/formato.js';
import { CampoBusqueda, Confirmar, EncabezadoPagina, Esqueleto, Insignia, Modal, Vacio } from '../../comun/ui.jsx';
import './inventario.css';

// Solo se muestran imágenes con una dirección https:// o una ruta propia; cualquier otra cosa se ignora.
const imagenSegura = (url) => (typeof url === 'string' && /^(https?:\/\/|\/)/.test(url) ? url : null);

function Miniatura({ producto }) {
  const url = imagenSegura(producto.imagenUrl);
  return url
    ? <img className="miniatura" src={url} alt="" loading="lazy" referrerPolicy="no-referrer" />
    : <span className="miniatura" aria-hidden="true">{producto.nombre.charAt(0).toUpperCase()}</span>;
}

// HU-010: carta del restaurante (categorías, productos y precios por presentación).
export default function Productos() {
  const [productos, setProductos] = useState(null);
  const [categorias, setCategorias] = useState([]);
  const [texto, setTexto] = useState('');
  const [categoriaId, setCategoriaId] = useState('');
  const [estado, setEstado] = useState('');
  const [error, setError] = useState('');
  const [aviso, setAviso] = useState('');
  const [modal, setModal] = useState(null);
  const [enviando, setEnviando] = useState(false);

  const cargar = useCallback(async () => {
    try {
      const [p, c] = await Promise.all([api('/productos'), api('/productos/categorias')]);
      setProductos(p);
      setCategorias(c);
      setError('');
    } catch (e) {
      setError(e.message);
    }
  }, []);
  useEffect(() => {
    cargar();
  }, [cargar]);

  async function ejecutar(accion, mensaje) {
    if (enviando) return;
    setEnviando(true);
    setError('');
    try {
      await accion();
      setAviso(mensaje);
      setModal(null);
      await cargar();
    } catch (e) {
      setError(e.message);
      setModal(null);
    } finally {
      setEnviando(false);
    }
  }

  const recargoLlevar = (id) => Number(categorias.find((c) => c.id === id)?.recargoParaLlevar ?? 0);
  const [vista, setVista] = useState('tarjetas');
  const visibles = (productos ?? []).filter((p) =>
    (!texto || p.nombre.toLowerCase().includes(texto.trim().toLowerCase())) &&
    (!categoriaId || String(p.categoriaId) === categoriaId) &&
    (!estado || (estado === 'activos' ? p.activo : !p.activo)));
  const activos = (productos ?? []).filter((p) => p.activo).length;
  const inactivos = (productos ?? []).length - activos;
  const presentacionesTotal = (productos ?? []).reduce((n, p) => n + (p.presentaciones?.length ?? 0), 0);

  const actualizarProducto = (p, cambios) => api(`/productos/${p.id}`, { metodo: 'PUT',
    cuerpo: { nombre: p.nombre, categoriaId: p.categoriaId, activo: p.activo, imagenUrl: p.imagenUrl ?? null, ...cambios } });

  return (
    <div>
      <EncabezadoPagina titulo="Productos" descripcion="Administre la carta: precios, categorías y disponibilidad.">
        <button type="button" className="boton" onClick={() => setModal({ tipo: 'nuevo' })}><Plus size={16} /> Nuevo producto</button>
        <button type="button" className="boton secundario" onClick={() => setModal({ tipo: 'categoria' })}><Tag size={16} /> Nueva categoría</button>
        <button type="button" className="boton secundario" disabled={!visibles.length} onClick={() => exportarCsv('productos', [
          { titulo: 'Producto', valor: (p) => p.nombre }, { titulo: 'Categoría', valor: (p) => p.categoria }, { titulo: 'Estado', valor: (p) => (p.activo ? 'Activo' : 'Inactivo') },
          { titulo: 'Precios', valor: (p) => p.presentaciones.map((x) => `${x.nombre}: ${Number(x.precio).toFixed(2)}`).join(' | ') }], visibles)}>
          <Download size={16} /> Exportar a Excel (CSV)
        </button>
      </EncabezadoPagina>
      {error && <p className="error" role="alert">{error}</p>}
      {aviso && <p className="aviso" role="status">{aviso}</p>}

      {productos && <section className="productos-resumen" aria-label="Resumen de productos">
        <div className="producto-resumen-card"><span className="producto-resumen-icono naranja"><Package size={20}/></span><div><small>Productos activos</small><strong>{activos}</strong></div></div>
        <div className="producto-resumen-card"><span className="producto-resumen-icono cafe"><Tag size={20}/></span><div><small>Categorías</small><strong>{categorias.length}</strong></div></div>
        <div className="producto-resumen-card"><span className="producto-resumen-icono neutro"><CircleOff size={20}/></span><div><small>Productos inactivos</small><strong>{inactivos}</strong></div></div>
        <div className="producto-resumen-card"><span className="producto-resumen-icono verde"><Layers3 size={20}/></span><div><small>Presentaciones registradas</small><strong>{presentacionesTotal}</strong></div></div>
      </section>}
      <div className="productos-filtros">
        <CampoBusqueda id="p-buscar" etiqueta="Buscar producto" placeholder="Buscar producto…" valor={texto} onChange={setTexto} />
        <div>
          <label htmlFor="p-categoria">Categoría</label>
          <select id="p-categoria" value={categoriaId} onChange={(e) => setCategoriaId(e.target.value)}>
            <option value="">Todas</option>
            {categorias.map((c) => <option key={c.id} value={c.id}>{c.nombre}</option>)}
          </select>
        </div>
        <div>
          <label htmlFor="p-estado">Estado</label>
          <select id="p-estado" value={estado} onChange={(e) => setEstado(e.target.value)}>
            <option value="">Todos</option><option value="activos">Activos</option><option value="inactivos">Inactivos</option>
          </select>
        </div>
        <div className="productos-vistas" role="group" aria-label="Vista de productos">
          <button type="button" className={vista === 'tarjetas' ? 'activo' : ''} aria-pressed={vista === 'tarjetas'} onClick={() => setVista('tarjetas')} title="Vista de tarjetas"><LayoutGrid size={17}/></button>
          <button type="button" className={vista === 'tabla' ? 'activo' : ''} aria-pressed={vista === 'tabla'} onClick={() => setVista('tabla')} title="Vista de tabla"><List size={17}/></button>
        </div>
      </div>

      {!productos ? <Esqueleto alto={240} /> : visibles.length === 0 ? <Vacio icono={Pizza} texto="No hay productos que coincidan." /> : (
        <>
          <div className="productos-resultados"><span>{visibles.length} producto(s)</span><span>{vista === 'tarjetas' ? 'Vista de tarjetas' : 'Vista de tabla'}</span></div>
          {vista === 'tarjetas' ? <div className="productos-grid">
            {visibles.map((p) => <article key={p.id} className={`producto-card ${p.activo ? '' : 'producto-card-inactivo'}`}>
              <div className="producto-card-imagen">
                {imagenSegura(p.imagenUrl) ? <img src={imagenSegura(p.imagenUrl)} alt={p.nombre} loading="lazy" referrerPolicy="no-referrer"/> : <div className="producto-placeholder"><Pizza size={38}/><span>{p.categoria}</span></div>}
                <span className={`producto-estado ${p.activo ? 'activo' : 'inactivo'}`}>{p.activo ? 'Activo' : 'Inactivo'}</span>
              </div>
              <div className="producto-card-cuerpo">
                <div className="producto-card-titulo"><div><small>{p.categoria}</small><h3>{p.nombre}</h3></div><span className="producto-codigo">#{p.id}</span></div>
                <div className="producto-precios">{p.presentaciones.map((x) => <div key={x.id}><span>{x.nombre}</span><strong>{soles(x.precio)}</strong></div>)}</div>
                <div className="producto-card-acciones">
                  <button type="button" className="boton secundario" onClick={() => setModal({ tipo: 'info', producto: p })}><Eye size={15}/> Ver detalle</button>
                  <button type="button" className="boton" onClick={() => setModal({ tipo: 'editar', producto: p })}><Pencil size={15}/> Editar</button>
                </div>
                <button type="button" className={`producto-activar ${p.activo ? 'desactivar' : 'activar'}`} onClick={() => setModal({ tipo: 'estado', producto: p })}>{p.activo ? 'Desactivar producto' : 'Activar producto'}</button>
              </div>
            </article>)}
          </div> : <div className="contenedor-tabla">
            <table className="tabla">
              <thead><tr><th>Producto</th><th>Categoría</th><th>Presentaciones y precios</th><th>Estado</th><th>Acciones</th></tr></thead>
              <tbody>
                {visibles.map((p) => (
                  <tr key={p.id} className={p.activo ? '' : 'inactivo'}>
                    <td><div className="celda-producto"><Miniatura producto={p} /><strong>{p.nombre}</strong></div></td>
                    <td>{p.categoria}</td>
                    <td>
                      {p.presentaciones.map((x) => (
                        <div key={x.id} className="precio-fila">
                          <span>{x.nombre}</span>
                          <button type="button" className="enlace" aria-label={`Actualizar precio de ${p.nombre} ${x.nombre}`} onClick={() => setModal({ tipo: 'precio', producto: p, presentacion: x })}>{soles(x.precio)}</button>
                        </div>
                      ))}
                    </td>
                    <td><Insignia tono={p.activo ? 'verde' : 'neutro'}>{p.activo ? 'Activo' : 'Inactivo'}</Insignia></td>
                    <td className="acciones-fila">
                      <button type="button" className="enlace" onClick={() => setModal({ tipo: 'info', producto: p })}><Eye size={14} /> Información</button>
                      <button type="button" className="enlace" onClick={() => setModal({ tipo: 'editar', producto: p })}><Pencil size={14} /> Editar</button>
                      <button type="button" className={p.activo ? 'enlace peligro' : 'enlace'} onClick={() => setModal({ tipo: 'estado', producto: p })}>{p.activo ? 'Desactivar' : 'Activar'}</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>}
        </>
      )}

      {modal?.tipo === 'nuevo' && <FormProducto categorias={categorias} enviando={enviando} onCerrar={() => setModal(null)}
        onGuardar={(d) => ejecutar(() => api('/productos', { metodo: 'POST', cuerpo: d }), 'Producto registrado.')} />}
      {modal?.tipo === 'editar' && <FormEditar producto={modal.producto} categorias={categorias} enviando={enviando} onCerrar={() => setModal(null)}
        onGuardar={(d) => ejecutar(() => actualizarProducto(modal.producto, d), 'Producto actualizado.')} />}
      {modal?.tipo === 'precio' && <FormPrecio {...modal} enviando={enviando} onCerrar={() => setModal(null)}
        onGuardar={(precio) => ejecutar(() => api(`/productos/presentaciones/${modal.presentacion.id}/precio`, { metodo: 'PUT', cuerpo: { precio } }), 'Precio actualizado. Los pedidos ya registrados conservan su precio.')} />}
      {modal?.tipo === 'estado' && (
        <Confirmar titulo={modal.producto.activo ? 'Desactivar producto' : 'Activar producto'} peligro={modal.producto.activo} enviando={enviando}
          textoBoton={modal.producto.activo ? 'Desactivar' : 'Activar'}
          mensaje={modal.producto.activo ? `«${modal.producto.nombre}» dejará de aparecer en la carta de pedidos. Los pedidos anteriores no se modifican.` : `«${modal.producto.nombre}» volverá a aparecer en la carta de pedidos.`}
          onConfirmar={() => ejecutar(() => actualizarProducto(modal.producto, { activo: !modal.producto.activo }), modal.producto.activo ? 'Producto desactivado.' : 'Producto activado.')}
          onCancelar={() => setModal(null)} />
      )}
      {modal?.tipo === 'categoria' && <FormCategoria enviando={enviando} onCerrar={() => setModal(null)}
        onGuardar={(d) => ejecutar(() => api('/productos/categorias', { metodo: 'POST', cuerpo: d }), 'Categoría registrada.')} />}
      {modal?.tipo === 'info' && (
        <Modal titulo={modal.producto.nombre} onCerrar={() => setModal(null)}>
          <dl className="detalle-lista">
            <dt>Categoría</dt><dd>{modal.producto.categoria}</dd>
            <dt>Estado</dt><dd>{modal.producto.activo ? 'Activo' : 'Inactivo'}</dd>
            <dt>Cargo para llevar</dt><dd>{recargoLlevar(modal.producto.categoriaId) > 0 ? `+${soles(recargoLlevar(modal.producto.categoriaId))} por unidad` : 'Sin cargo'}</dd>
            {modal.producto.presentaciones.map((x) => (
              <><dt key={`t${x.id}`}>{x.nombre}</dt><dd key={`d${x.id}`}>{soles(x.precio)}{x.recargoExtraQueso != null ? ` · extra queso +${soles(x.recargoExtraQueso)}` : ' · sin extra queso'}</dd></>
            ))}
          </dl>
        </Modal>
      )}
    </div>
  );
}

function CampoImagen({ valor, onChange }) {
  return (
    <>
      <label htmlFor="pr-imagen">Imagen (opcional, dirección https://…)</label>
      <input id="pr-imagen" type="url" placeholder="https://…" value={valor} onChange={(e) => onChange(e.target.value)} maxLength={300} pattern="(https?://|/).*" />
    </>
  );
}

function FormProducto({ categorias, onGuardar, onCerrar, enviando }) {
  const [d, setD] = useState({ nombre: '', categoriaId: categorias[0]?.id ?? '', imagenUrl: '', presentaciones: [{ nombre: '', precio: '', recargoExtraQueso: '' }] });
  const cambiar = (i, campo, v) => setD({ ...d, presentaciones: d.presentaciones.map((p, k) => (k === i ? { ...p, [campo]: v } : p)) });
  return (
    <Modal titulo="Nuevo producto" onCerrar={onCerrar}>
      <form className="formulario" style={{ maxWidth: 'none' }} onSubmit={(e) => {
        e.preventDefault();
        onGuardar({ nombre: d.nombre, categoriaId: Number(d.categoriaId), imagenUrl: d.imagenUrl || null,
          presentaciones: d.presentaciones.map((p) => ({ nombre: p.nombre, precio: Number(p.precio), recargoExtraQueso: p.recargoExtraQueso === '' ? null : Number(p.recargoExtraQueso) })) });
      }}>
        <label htmlFor="np-nombre">Nombre</label>
        <input id="np-nombre" value={d.nombre} onChange={(e) => setD({ ...d, nombre: e.target.value })} required maxLength={80} />
        <label htmlFor="np-categoria">Categoría</label>
        <select id="np-categoria" value={d.categoriaId} onChange={(e) => setD({ ...d, categoriaId: e.target.value })} required>
          {categorias.map((c) => <option key={c.id} value={c.id}>{c.nombre}</option>)}
        </select>
        <CampoImagen valor={d.imagenUrl} onChange={(v) => setD({ ...d, imagenUrl: v })} />
        <strong className="subtitulo">Presentaciones y precios</strong>
        {d.presentaciones.map((p, i) => (
          <div key={i} className="fila-formulario">
            <input aria-label={`Presentación ${i + 1}`} placeholder="Ej.: Personal" value={p.nombre} onChange={(e) => cambiar(i, 'nombre', e.target.value)} required />
            <input aria-label={`Precio ${i + 1}`} type="number" step="0.01" min="0.01" placeholder="Precio" value={p.precio} onChange={(e) => cambiar(i, 'precio', e.target.value)} required />
            <input aria-label={`Extra queso ${i + 1}`} type="number" step="0.01" min="0.01" placeholder="Extra queso (opcional)" value={p.recargoExtraQueso} onChange={(e) => cambiar(i, 'recargoExtraQueso', e.target.value)} />
          </div>
        ))}
        <button type="button" className="enlace" onClick={() => setD({ ...d, presentaciones: [...d.presentaciones, { nombre: '', precio: '', recargoExtraQueso: '' }] })}>+ Otra presentación</button>
        <div className="acciones"><button className="boton" type="submit" disabled={enviando}>Guardar</button><button type="button" className="boton secundario" onClick={onCerrar}>Cancelar</button></div>
      </form>
    </Modal>
  );
}

function FormEditar({ producto, categorias, onGuardar, onCerrar, enviando }) {
  const [d, setD] = useState({ nombre: producto.nombre, categoriaId: producto.categoriaId, imagenUrl: producto.imagenUrl ?? '' });
  return (
    <Modal titulo={`Editar: ${producto.nombre}`} onCerrar={onCerrar} estrecho>
      <form className="formulario" onSubmit={(e) => { e.preventDefault(); onGuardar({ nombre: d.nombre, categoriaId: Number(d.categoriaId), imagenUrl: d.imagenUrl || null }); }}>
        <label htmlFor="ep-nombre">Nombre</label>
        <input id="ep-nombre" value={d.nombre} onChange={(e) => setD({ ...d, nombre: e.target.value })} required maxLength={80} />
        <label htmlFor="ep-categoria">Categoría</label>
        <select id="ep-categoria" value={d.categoriaId} onChange={(e) => setD({ ...d, categoriaId: e.target.value })}>
          {categorias.map((c) => <option key={c.id} value={c.id}>{c.nombre}</option>)}
        </select>
        <CampoImagen valor={d.imagenUrl} onChange={(v) => setD({ ...d, imagenUrl: v })} />
        <div className="acciones"><button className="boton" type="submit" disabled={enviando}>Guardar</button><button type="button" className="boton secundario" onClick={onCerrar}>Cancelar</button></div>
      </form>
    </Modal>
  );
}

function FormPrecio({ producto, presentacion, onGuardar, onCerrar, enviando }) {
  const [precio, setPrecio] = useState(String(presentacion.precio));
  return (
    <Modal titulo="Actualizar precio" onCerrar={onCerrar} estrecho>
      <p>{producto.nombre} · {presentacion.nombre}. Precio actual: <strong>{soles(presentacion.precio)}</strong></p>
      <form className="formulario" onSubmit={(e) => { e.preventDefault(); onGuardar(Number(precio)); }}>
        <label htmlFor="pp-precio">Nuevo precio (S/)</label>
        <input id="pp-precio" type="number" step="0.01" min="0.01" value={precio} onChange={(e) => setPrecio(e.target.value)} required autoFocus />
        <div className="acciones"><button className="boton" type="submit" disabled={enviando}>Guardar</button><button type="button" className="boton secundario" onClick={onCerrar}>Cancelar</button></div>
      </form>
    </Modal>
  );
}

function FormCategoria({ onGuardar, onCerrar, enviando }) {
  const [d, setD] = useState({ nombre: '', recargoParaLlevar: '' });
  return (
    <Modal titulo="Nueva categoría" onCerrar={onCerrar} estrecho>
      <form className="formulario" onSubmit={(e) => { e.preventDefault(); onGuardar({ nombre: d.nombre, recargoParaLlevar: d.recargoParaLlevar === '' ? null : Number(d.recargoParaLlevar) }); }}>
        <label htmlFor="nc-nombre">Nombre</label>
        <input id="nc-nombre" value={d.nombre} onChange={(e) => setD({ ...d, nombre: e.target.value })} required maxLength={50} />
        <label htmlFor="nc-llevar">Cargo para llevar (S/ por unidad, opcional)</label>
        <input id="nc-llevar" type="number" step="0.01" min="0" value={d.recargoParaLlevar} onChange={(e) => setD({ ...d, recargoParaLlevar: e.target.value })} />
        <div className="acciones"><button className="boton" type="submit" disabled={enviando}>Guardar</button><button type="button" className="boton secundario" onClick={onCerrar}>Cancelar</button></div>
      </form>
    </Modal>
  );
}
