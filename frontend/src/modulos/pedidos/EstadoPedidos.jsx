import { useCallback, useEffect, useMemo, useState } from 'react';
import { api } from '../../comun/api.js';
import { useAuth } from '../seguridad/auth.jsx';
import { soles } from '../../comun/formato.js';
import { ClipboardList, RefreshCw, Search, Clock3, CookingPot, CheckCircle2, ArrowRight } from 'lucide-react';
import './pedidos.css';
import './mozo-redisenio.css';

const COLUMNAS = [
  { estado: 'PENDIENTE', titulo: 'Pendientes', siguiente: 'EN_PREPARACION', boton: 'Enviar a preparación', icono: Clock3 },
  { estado: 'EN_PREPARACION', titulo: 'En preparación', siguiente: 'ATENDIDO', boton: 'Marcar atendido', icono: CookingPot },
  { estado: 'ATENDIDO', titulo: 'Atendidos', siguiente: null, boton: null, icono: CheckCircle2 },
];

export default function EstadoPedidos() {
  const { tienePermiso } = useAuth();
  const [pedidos, setPedidos] = useState([]);
  const [error, setError] = useState('');
  const [busqueda, setBusqueda] = useState('');
  const [actualizando, setActualizando] = useState(false);

  const cargar = useCallback(async () => {
    setActualizando(true);
    try {
      const datos = await api('/pedidos');
      setPedidos(Array.isArray(datos) ? datos : []);
      setError('');
    } catch (e) {
      setError(e.message || 'No se pudieron cargar los pedidos.');
    } finally { setActualizando(false); }
  }, []);

  useEffect(() => {
    cargar();
    const reloj = setInterval(cargar, 10000);
    return () => clearInterval(reloj);
  }, [cargar]);

  async function avanzar(pedido, siguiente) {
    try {
      await api(`/pedidos/${pedido.id}/estado`, { metodo: 'PATCH', cuerpo: { estado: siguiente } });
      await cargar();
    } catch (e) { setError(e.message || 'No se pudo actualizar el estado.'); }
  }

  const hora = (f) => new Date(f).toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' });
  const encontrados = useMemo(() => pedidos.filter(p => `${p.codigo || ''} ${p.mesa || ''} ${p.mozo || ''} ${p.detalles?.map(d => d.producto).join(' ') || ''}`.toLowerCase().includes(busqueda.trim().toLowerCase())), [pedidos, busqueda]);
  const cuenta = (estado) => encontrados.filter(p => p.estado === estado).length;

  return <div className="mozo-pedidos">
    <header className="ep-header"><span className="ep-icon"><ClipboardList size={26}/></span><div><span className="ep-eyebrow">SEGUIMIENTO DEL SERVICIO</span><h2>Pedidos en curso</h2><p>Revisa el avance y actualiza los pedidos según su preparación.</p></div><button type="button" className="boton secundario ep-refresh" onClick={cargar} disabled={actualizando}><RefreshCw size={16} className={actualizando ? 'mesas-girando' : ''}/>{actualizando ? 'Actualizando…' : 'Actualizar'}</button></header>
    {error && <p className="error" role="alert">{error}</p>}
    <div className="ep-resumen">
      <article><span className="ep-res-icon pendiente"><Clock3 size={18}/></span><div><small>Pendientes</small><strong>{cuenta('PENDIENTE')}</strong></div></article>
      <article><span className="ep-res-icon preparacion"><CookingPot size={18}/></span><div><small>En preparación</small><strong>{cuenta('EN_PREPARACION')}</strong></div></article>
      <article><span className="ep-res-icon atendido"><CheckCircle2 size={18}/></span><div><small>Atendidos</small><strong>{cuenta('ATENDIDO')}</strong></div></article>
      <article><span className="ep-res-icon total"><ClipboardList size={18}/></span><div><small>Total de pedidos</small><strong>{encontrados.length}</strong></div></article>
    </div>
    <div className="ep-toolbar"><div className="ep-search"><Search size={17}/><input aria-label="Buscar pedidos" placeholder="Buscar por código, mesa, mozo o producto…" value={busqueda} onChange={e => setBusqueda(e.target.value)}/></div><span className="ep-actualizacion">Se actualiza automáticamente cada 10 segundos</span></div>
    <div className="ep-columnas">
      {COLUMNAS.map(col => { const lista = encontrados.filter(p => p.estado === col.estado); const Icon = col.icono;
        return <section key={col.estado} className={`ep-columna ${col.estado.toLowerCase()}`} aria-label={col.titulo}>
          <div className="ep-col-cabecera"><span className="ep-col-icon"><Icon size={17}/></span><h3>{col.titulo}</h3><span className="ep-contador">{lista.length}</span></div>
          {lista.length === 0 ? <div className="ep-vacio"><ClipboardList size={25}/><span>{busqueda ? 'No hay coincidencias' : 'Sin pedidos'}</span></div> : lista.map(p => <article key={p.id} className="ep-pedido">
            <div className="ep-pedido-head"><strong>{p.codigo}</strong><span><Clock3 size={13}/>{hora(p.fechaHora)}</span></div><div className="ep-mesa"><span>Mesa {p.mesa}</span><small>{p.mozo || 'Mozo'}</small></div>
            <ul className="ep-items">{p.detalles.map((d, i) => <li key={i}><b>{d.cantidad}×</b><span>{d.producto} <small>({d.presentacion})</small>{d.extraQueso ? ' · extra queso' : ''}{d.paraLlevar ? ' · para llevar' : ''}</span></li>)}</ul>
            <div className="ep-total"><span>Total del pedido</span><strong>{soles(p.total)}</strong></div>
            {col.siguiente && tienePermiso('PEDIDOS_ESTADO') && <button type="button" className="boton ep-avanzar" onClick={() => avanzar(p, col.siguiente)}>{col.boton}<ArrowRight size={16}/></button>}
          </article>)}
        </section>;
      })}
    </div>
  </div>;
}
