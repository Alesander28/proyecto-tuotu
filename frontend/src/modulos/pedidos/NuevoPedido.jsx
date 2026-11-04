import { useEffect, useState } from 'react';
import { api } from '../../comun/api.js';
import { soles } from '../../comun/formato.js';
import './pedidos.css';

// HU-007: el mozo arma el pedido desde la carta. El servidor vuelve a calcular el total con los precios reales.
export default function NuevoPedido() {
  const [carta, setCarta] = useState(null);
  const [categoriaId, setCategoriaId] = useState(null);
  const [mesa, setMesa] = useState('');
  const [lineas, setLineas] = useState([]);
  const [error, setError] = useState('');
  const [aviso, setAviso] = useState('');
  const [enviando, setEnviando] = useState(false);

  useEffect(() => {
    api('/carta')
      .then((datos) => {
        setCarta(datos);
        setCategoriaId(datos.categorias[0]?.id ?? null);
      })
      .catch((e) => setError(e.message));
  }, []);

  if (!carta) return <p>{error || 'Cargando la carta…'}</p>;

  const precioUnitario = (l) =>
    Number(l.precio) + (l.extraQueso ? Number(carta.recargoExtraQueso) : 0) + (l.paraLlevar ? Number(carta.recargoParaLlevar) : 0);
  const total = lineas.reduce((suma, l) => suma + precioUnitario(l) * l.cantidad, 0);

  function agregar(producto, presentacion) {
    setAviso('');
    setLineas((actuales) => {
      const existente = actuales.find((l) => l.presentacionId === presentacion.id);
      if (existente) {
        return actuales.map((l) => (l === existente ? { ...l, cantidad: l.cantidad + 1 } : l));
      }
      return [...actuales, {
        presentacionId: presentacion.id,
        nombre: `${producto.nombre} (${presentacion.nombre})`,
        precio: presentacion.precio,
        cantidad: 1,
        paraLlevar: false,
        extraQueso: false,
      }];
    });
  }

  const cambiar = (id, cambios) =>
    setLineas((actuales) => actuales.map((l) => (l.presentacionId === id ? { ...l, ...cambios } : l)));
  const quitar = (id) => setLineas((actuales) => actuales.filter((l) => l.presentacionId !== id));

  async function enviar() {
    setError('');
    setAviso('');
    if (!mesa.trim()) return setError('Indique la mesa.');
    if (lineas.length === 0) return setError('Agregue al menos un producto.');
    setEnviando(true);
    try {
      const pedido = await api('/pedidos', {
        metodo: 'POST',
        cuerpo: {
          mesa: mesa.trim(),
          items: lineas.map((l) => ({
            presentacionId: l.presentacionId, cantidad: l.cantidad, paraLlevar: l.paraLlevar, extraQueso: l.extraQueso,
          })),
        },
      });
      setAviso(`Pedido ${pedido.codigo} registrado (mesa ${pedido.mesa}) por ${soles(pedido.total)}. Estado: pendiente.`);
      setLineas([]);
      setMesa('');
    } catch (e) {
      setError(e.message);
    } finally {
      setEnviando(false);
    }
  }

  const categoria = carta.categorias.find((c) => c.id === categoriaId);

  return (
    <div>
      <h2>Nuevo pedido</h2>
      {aviso && <p className="aviso" role="status">{aviso}</p>}
      {error && <p className="error" role="alert">{error}</p>}

      <div className="pedido-pantalla">
        <section className="carta" aria-label="Carta">
          <div className="categorias" role="tablist">
            {carta.categorias.map((c) => (
              <button key={c.id} role="tab" aria-selected={c.id === categoriaId}
                      className={c.id === categoriaId ? 'chip activo' : 'chip'} onClick={() => setCategoriaId(c.id)}>
                {c.nombre}
              </button>
            ))}
          </div>
          {categoria?.productos.map((p) => (
            <div key={p.id} className="producto">
              <strong>{p.nombre}</strong>
              <div className="presentaciones">
                {p.presentaciones.map((pr) => (
                  <button key={pr.id} className="boton ligero" onClick={() => agregar(p, pr)}
                          aria-label={`Agregar ${p.nombre} ${pr.nombre}`}>
                    {pr.nombre} {soles(pr.precio)}
                  </button>
                ))}
              </div>
            </div>
          ))}
        </section>

        <aside className="tarjeta resumen" aria-label="Pedido actual">
          <h3>Pedido actual</h3>
          <label htmlFor="mesa">Mesa</label>
          <input id="mesa" value={mesa} onChange={(e) => setMesa(e.target.value)} maxLength={10} />

          {lineas.length === 0 && <p className="nota">Aún no hay productos.</p>}
          {lineas.map((l) => (
            <div key={l.presentacionId} className="linea">
              <div className="linea-titulo">
                <span>{l.nombre}</span>
                <span>{soles(precioUnitario(l) * l.cantidad)}</span>
              </div>
              <div className="linea-controles">
                <button className="boton ligero" aria-label={`Quitar una unidad de ${l.nombre}`}
                        onClick={() => (l.cantidad > 1 ? cambiar(l.presentacionId, { cantidad: l.cantidad - 1 }) : quitar(l.presentacionId))}>−</button>
                <span aria-label="Cantidad">{l.cantidad}</span>
                <button className="boton ligero" aria-label={`Sumar una unidad de ${l.nombre}`}
                        onClick={() => cambiar(l.presentacionId, { cantidad: l.cantidad + 1 })}>+</button>
                <label className="check">
                  <input type="checkbox" checked={l.extraQueso}
                         onChange={(e) => cambiar(l.presentacionId, { extraQueso: e.target.checked })} />
                  Extra queso (+{soles(carta.recargoExtraQueso)})
                </label>
                <label className="check">
                  <input type="checkbox" checked={l.paraLlevar}
                         onChange={(e) => cambiar(l.presentacionId, { paraLlevar: e.target.checked })} />
                  Para llevar (+{soles(carta.recargoParaLlevar)})
                </label>
              </div>
            </div>
          ))}

          <div className="total">
            <span>Total</span>
            <strong data-testid="total">{soles(total)}</strong>
          </div>
          <button className="boton" onClick={enviar} disabled={enviando}>
            {enviando ? 'Enviando…' : 'Enviar pedido'}
          </button>
        </aside>
      </div>
    </div>
  );
}
