import { useEffect, useState } from 'react';
import { ShoppingBasket, ReceiptText, UtensilsCrossed, Search } from 'lucide-react';
import { useSearchParams } from 'react-router-dom';
import { api } from '../../comun/api.js';
import { soles } from '../../comun/formato.js';
import './pedidos.css';
import './mozo-redisenio.css';

// HU-007: el mozo arma el pedido desde la carta y selecciona una mesa
// registrada y disponible en la gestión de mesas del gerente.
export default function NuevoPedido() {
  const [searchParams] = useSearchParams();
  const [carta, setCarta] = useState(null);
  const [categoriaId, setCategoriaId] = useState(null);
  const [mesas, setMesas] = useState([]);
  const [cargandoMesas, setCargandoMesas] = useState(true);
  const [mesa, setMesa] = useState('');
  const [lineas, setLineas] = useState([]);
  const [error, setError] = useState('');
  const [errorMesas, setErrorMesas] = useState('');
  const [aviso, setAviso] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [busquedaProducto, setBusquedaProducto] = useState('');

  useEffect(() => {
    let vigente = true;
    const mesaSolicitada = searchParams.get('mesa');

    api('/carta')
      .then((datos) => {
        if (!vigente) return;
        setCarta(datos);
        setCategoriaId(datos.categorias[0]?.id ?? null);
      })
      .catch((e) => { if (vigente) setError(e.message); });

    // /api/mesas devuelve las mesas creadas por el gerente junto con su estado.
    api('/mesas')
      .then((datos) => {
        if (!vigente) return;
        const disponibles = (Array.isArray(datos) ? datos : [])
          .filter((m) => m.activa && m.estado === 'LIBRE')
          .sort((a, b) => String(a.numero).localeCompare(String(b.numero), 'es', { numeric: true }));
        setMesas(disponibles);
        if (mesaSolicitada) {
          const coincidencia = disponibles.find((m) => String(m.numero).toLowerCase() === mesaSolicitada.toLowerCase());
          if (coincidencia) setMesa(coincidencia.numero);
        }
        setErrorMesas('');
      })
      .catch((e) => { if (vigente) setErrorMesas(e.message || 'No se pudieron cargar las mesas.'); })
      .finally(() => { if (vigente) setCargandoMesas(false); });

    return () => { vigente = false; };
  }, [searchParams]);

  if (!carta) return <p>{error || 'Cargando la carta…'}</p>;

  const precioUnitario = (l) =>
    Number(l.precio) + (l.extraQueso ? Number(l.recargoExtraQueso ?? 0) : 0) + (l.paraLlevar ? Number(l.recargoParaLlevar ?? 0) : 0);
  const total = lineas.reduce((suma, l) => suma + precioUnitario(l) * l.cantidad, 0);

  function agregar(producto, presentacion, categoriaDelProducto) {
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
        recargoExtraQueso: presentacion.recargoExtraQueso,
        recargoParaLlevar: categoriaDelProducto.recargoParaLlevar,
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
    if (!mesa) return setError('Seleccione una mesa disponible.');
    if (lineas.length === 0) return setError('Agregue al menos un producto.');
    setEnviando(true);
    try {
      const pedido = await api('/pedidos', {
        metodo: 'POST',
        cuerpo: {
          mesa,
          items: lineas.map((l) => ({
            presentacionId: l.presentacionId, cantidad: l.cantidad, paraLlevar: l.paraLlevar, extraQueso: l.extraQueso,
          })),
        },
      });
      setAviso(`Pedido ${pedido.codigo} registrado (mesa ${pedido.mesa}) por ${soles(pedido.total)}. Estado: pendiente.`);
      setLineas([]);
      setMesa('');
      // La mesa queda ocupada al registrarse el pedido. Se retira de las opciones.
      setMesas((actuales) => actuales.filter((m) => String(m.numero) !== String(pedido.mesa)));
    } catch (e) {
      setError(e.message);
    } finally {
      setEnviando(false);
    }
  }

  const categoria = carta.categorias.find((c) => c.id === categoriaId);

  return (
    <div className="mozo-nuevo-pedido">
      <header className="np-header"><span className="np-header-icon"><ShoppingBasket size={26}/></span><div><span className="np-eyebrow">SERVICIO · TOMA DE PEDIDOS</span><h2>Nuevo pedido</h2><p>Selecciona una mesa y agrega los productos que solicitaron tus clientes.</p></div><span className="np-header-chip"><UtensilsCrossed size={15}/> Carta disponible</span></header>
      {aviso && <p className="aviso" role="status">{aviso}</p>}
      {error && <p className="error" role="alert">{error}</p>}

      <div className="pedido-pantalla np-layout">
        <section className="carta np-carta-panel tarjeta" aria-label="Carta">
          <div className="np-panel-cabecera"><div><h3>Menú del restaurante</h3><p>Elige una categoría y agrega presentaciones.</p></div><span className="np-carta-tag">Carta</span></div>
          <div className="np-buscador"><Search size={17}/><input aria-label="Buscar producto" placeholder="Buscar producto en la categoría…" value={busquedaProducto} onChange={(e)=>setBusquedaProducto(e.target.value)}/></div>
          <div className="categorias np-categorias" role="tablist">
            {carta.categorias.map((c) => (
              <button key={c.id} role="tab" aria-selected={c.id === categoriaId}
                      className={c.id === categoriaId ? 'chip np-categoria activa' : 'chip np-categoria'} onClick={() => {setCategoriaId(c.id);setBusquedaProducto('');}}>
                {c.nombre}
              </button>
            ))}
          </div>
          <div className="np-productos">
          {categoria?.productos.filter(p=>p.nombre.toLowerCase().includes(busquedaProducto.trim().toLowerCase())).map((p) => (
            <div key={p.id} className="producto np-producto">
              <div className="np-producto-info"><span className="np-producto-icon"><UtensilsCrossed size={17}/></span><div><strong>{p.nombre}</strong><small>{p.presentaciones.length} presentación(es)</small></div></div>
              <div className="presentaciones">
                {p.presentaciones.map((pr) => (
                  <button key={pr.id} className="boton ligero np-agregar" onClick={() => agregar(p, pr, categoria)}
                          aria-label={`Agregar ${p.nombre} ${pr.nombre}`}>
                    {pr.nombre} {soles(pr.precio)}
                  </button>
                ))}
              </div>
            </div>
          ))}
          {categoria?.productos.filter(p=>p.nombre.toLowerCase().includes(busquedaProducto.trim().toLowerCase())).length===0 && <div className="np-vacio"><Search size={25}/><strong>No encontramos productos</strong><p>Prueba con otra búsqueda.</p></div>}
          </div>
        </section>

        <aside className="tarjeta resumen np-carrito" aria-label="Pedido actual">
          <div className="np-carrito-cabecera"><span className="np-carrito-icon"><ReceiptText size={20}/></span><div><h3>Tu pedido</h3><p>{lineas.reduce((n,l)=>n+l.cantidad,0)} artículo(s)</p></div></div>
          <div className="np-mesa-selector"><label htmlFor="mesa">Mesa de atención</label>
          <select
            id="mesa" aria-label="Mesa"
            value={mesa}
            onChange={(e) => setMesa(e.target.value)}
            disabled={cargandoMesas || mesas.length === 0}
            required
          >
            <option value="">{cargandoMesas ? 'Cargando mesas…' : 'Selecciona una mesa'}</option>
            {mesas.map((m) => (
              <option key={m.id} value={m.numero}>Mesa {m.numero} — Disponible</option>
            ))}
          </select>
          {errorMesas && <p className="error" role="alert">{errorMesas}</p>}
          {!cargandoMesas && !errorMesas && mesas.length === 0 && (
            <p className="nota">No hay mesas disponibles. Solicita al gerente que registre y active una mesa.</p>
          )}</div>
          <div className="np-lineas">

          {lineas.length === 0 && <div className="np-carrito-vacio"><ShoppingBasket size={28}/><strong>Tu pedido está vacío</strong><p>Agrega productos desde la carta.</p></div>}
          {lineas.map((l) => (
            <div key={l.presentacionId} className="linea np-linea">
              <div className="linea-titulo">
                <span>{l.nombre}</span>
                <span>{soles(precioUnitario(l) * l.cantidad)}</span>
              </div>
              <div className="linea-controles">
                <button className="boton ligero np-cantidad-btn" aria-label={`Quitar una unidad de ${l.nombre}`}
                        onClick={() => (l.cantidad > 1 ? cambiar(l.presentacionId, { cantidad: l.cantidad - 1 }) : quitar(l.presentacionId))}>−</button>
                <span aria-label="Cantidad">{l.cantidad}</span>
                <button className="boton ligero np-cantidad-btn" aria-label={`Sumar una unidad de ${l.nombre}`}
                        onClick={() => cambiar(l.presentacionId, { cantidad: l.cantidad + 1 })}>+</button>
                {l.recargoExtraQueso != null && (
                  <label className="check">
                    <input type="checkbox" checked={l.extraQueso}
                           onChange={(e) => cambiar(l.presentacionId, { extraQueso: e.target.checked })} />
                    Extra queso (+{soles(l.recargoExtraQueso)})
                  </label>
                )}
                <label className="check">
                  <input type="checkbox" checked={l.paraLlevar}
                         onChange={(e) => cambiar(l.presentacionId, { paraLlevar: e.target.checked })} />
                  Para llevar{Number(l.recargoParaLlevar) > 0 ? ` (+${soles(l.recargoParaLlevar)})` : ''}
                </label>
              </div>
            </div>
          ))}
          </div>

          <div className="total np-total">
            <span>Total</span>
            <strong data-testid="total">{soles(total)}</strong>
          </div>
          <button className="boton np-enviar" onClick={enviar} disabled={enviando || cargandoMesas || !mesa || lineas.length === 0}>
            {enviando ? 'Enviando…' : 'Enviar pedido'}
          </button>
          <small className="np-seguridad">Verifica mesa y productos antes de enviar.</small>
        </aside>
      </div>
    </div>
  );
}
