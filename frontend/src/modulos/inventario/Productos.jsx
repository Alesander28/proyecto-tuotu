import { useCallback, useEffect, useState } from 'react';
import { api } from '../../comun/api.js';
import { soles } from '../../comun/formato.js';
import './inventario.css';

const NUEVO = { nombre: '', categoriaId: '', presentaciones: [{ nombre: '', precio: '' }] };

// HU-010: el gerente administra la carta: categorías, productos y precios por presentación.
export default function Productos() {
  const [productos, setProductos] = useState([]);
  const [categorias, setCategorias] = useState([]);
  const [formulario, setFormulario] = useState(null);
  const [categoriaNueva, setCategoriaNueva] = useState('');
  const [precios, setPrecios] = useState({}); // id de presentación -> texto en edición
  const [error, setError] = useState('');
  const [aviso, setAviso] = useState('');

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
    setError('');
    setAviso('');
    try {
      await accion();
      if (mensaje) setAviso(mensaje);
      await cargar();
    } catch (e) {
      setError(e.message);
    }
  }

  const abrirNuevo = () => setFormulario({ ...NUEVO, categoriaId: categorias[0]?.id ?? '', presentaciones: [{ nombre: '', precio: '' }] });

  const guardarProducto = (evento) => {
    evento.preventDefault();
    return ejecutar(async () => {
      await api('/productos', {
        metodo: 'POST',
        cuerpo: {
          nombre: formulario.nombre,
          categoriaId: Number(formulario.categoriaId),
          presentaciones: formulario.presentaciones.map((p) => ({ nombre: p.nombre, precio: Number(p.precio) })),
        },
      });
      setFormulario(null);
    }, 'Producto registrado.');
  };

  const guardarCategoria = (evento) => {
    evento.preventDefault();
    return ejecutar(async () => {
      await api('/productos/categorias', { metodo: 'POST', cuerpo: { nombre: categoriaNueva } });
      setCategoriaNueva('');
    }, 'Categoría registrada.');
  };

  const cambiarActivo = (p) =>
    ejecutar(() => api(`/productos/${p.id}`, {
      metodo: 'PUT', cuerpo: { nombre: p.nombre, categoriaId: p.categoriaId, activo: !p.activo },
    }), p.activo ? 'Producto desactivado: ya no aparece en la carta de pedidos.' : 'Producto activado.');

  const guardarPrecio = (pr) =>
    ejecutar(async () => {
      await api(`/productos/presentaciones/${pr.id}/precio`, { metodo: 'PUT', cuerpo: { precio: Number(precios[pr.id]) } });
      setPrecios((actual) => {
        const { [pr.id]: _quitado, ...resto } = actual;
        return resto;
      });
    }, 'Precio actualizado. Los pedidos ya registrados conservan su precio.');

  const cambiarPresentacion = (i, campo, valor) =>
    setFormulario({ ...formulario, presentaciones: formulario.presentaciones.map((p, k) => (k === i ? { ...p, [campo]: valor } : p)) });

  return (
    <div>
      <div className="fila-titulo">
        <h2>Productos y precios</h2>
        <button className="boton" onClick={abrirNuevo}>+ Nuevo producto</button>
      </div>
      {error && <p className="error" role="alert">{error}</p>}
      {aviso && <p className="aviso" role="status">{aviso}</p>}

      <form className="fila-formulario" onSubmit={guardarCategoria}>
        <label htmlFor="cat-nueva" className="solo-lectores">Nueva categoría</label>
        <input id="cat-nueva" placeholder="Nueva categoría" value={categoriaNueva}
               onChange={(e) => setCategoriaNueva(e.target.value)} required />
        <button className="boton secundario" type="submit">Agregar categoría</button>
      </form>

      {formulario && (
        <form className="tarjeta formulario" onSubmit={guardarProducto}>
          <h3>Nuevo producto</h3>
          <label htmlFor="p-nombre">Nombre</label>
          <input id="p-nombre" value={formulario.nombre} onChange={(e) => setFormulario({ ...formulario, nombre: e.target.value })} required />
          <label htmlFor="p-categoria">Categoría</label>
          <select id="p-categoria" value={formulario.categoriaId} onChange={(e) => setFormulario({ ...formulario, categoriaId: e.target.value })} required>
            {categorias.map((c) => <option key={c.id} value={c.id}>{c.nombre}</option>)}
          </select>
          <strong className="subtitulo">Presentaciones y precios</strong>
          {formulario.presentaciones.map((p, i) => (
            <div key={i} className="fila-formulario">
              <input aria-label={`Presentación ${i + 1}`} placeholder="Ej.: Personal" value={p.nombre}
                     onChange={(e) => cambiarPresentacion(i, 'nombre', e.target.value)} required />
              <input aria-label={`Precio ${i + 1}`} type="number" step="0.01" min="0.01" placeholder="Precio" value={p.precio}
                     onChange={(e) => cambiarPresentacion(i, 'precio', e.target.value)} required />
            </div>
          ))}
          <button type="button" className="enlace"
                  onClick={() => setFormulario({ ...formulario, presentaciones: [...formulario.presentaciones, { nombre: '', precio: '' }] })}>
            + Otra presentación
          </button>
          <div className="acciones">
            <button className="boton" type="submit">Guardar</button>
            <button className="boton secundario" type="button" onClick={() => setFormulario(null)}>Cancelar</button>
          </div>
        </form>
      )}

      <table className="tabla">
        <thead>
          <tr><th>Categoría</th><th>Producto</th><th>Presentaciones y precios</th><th>Estado</th></tr>
        </thead>
        <tbody>
          {productos.map((p) => (
            <tr key={p.id} className={p.activo ? '' : 'inactivo'}>
              <td>{p.categoria}</td>
              <td>{p.nombre}</td>
              <td>
                {p.presentaciones.map((pr) => (
                  <div key={pr.id} className="precio-fila">
                    <span>{pr.nombre}</span>
                    {precios[pr.id] === undefined ? (
                      <button className="enlace" aria-label={`Cambiar precio de ${p.nombre} ${pr.nombre}`}
                              onClick={() => setPrecios({ ...precios, [pr.id]: String(pr.precio) })}>{soles(pr.precio)}</button>
                    ) : (
                      <>
                        <input type="number" step="0.01" min="0.01" aria-label={`Nuevo precio de ${p.nombre} ${pr.nombre}`}
                               value={precios[pr.id]} onChange={(e) => setPrecios({ ...precios, [pr.id]: e.target.value })} />
                        <button className="enlace" onClick={() => guardarPrecio(pr)}>Guardar</button>
                      </>
                    )}
                  </div>
                ))}
              </td>
              <td>
                {p.activo ? 'Activo' : 'Inactivo'}{' '}
                <button className="enlace" onClick={() => cambiarActivo(p)}>{p.activo ? 'Desactivar' : 'Activar'}</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
