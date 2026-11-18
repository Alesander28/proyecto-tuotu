import { useCallback, useEffect, useMemo, useState } from 'react';
import { Armchair, Plus, RefreshCw, Power, History, X, Search, Users, CircleCheck, CircleAlert, CircleOff, Clock3, ClipboardList } from 'lucide-react';
import { api } from '../../comun/api.js';

const dinero = (valor) => `S/ ${Number(valor || 0).toFixed(2)}`;
const fecha = (valor) => valor ? new Date(valor).toLocaleString('es-PE', { dateStyle: 'short', timeStyle: 'short' }) : '—';

export default function Mesas() {
  const [mesas, setMesas] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState('');
  const [mensaje, setMensaje] = useState('');
  const [numero, setNumero] = useState('');
  const [guardando, setGuardando] = useState(false);
  const [modalNueva, setModalNueva] = useState(false);
  const [historial, setHistorial] = useState(null);
  const [busqueda, setBusqueda] = useState('');
  const [filtro, setFiltro] = useState('TODAS');

  const cargar = useCallback(async () => {
    setCargando(true); setError('');
    try { setMesas(await api('/mesas') || []); }
    catch (e) { setError(e.message || 'No se pudieron cargar las mesas.'); }
    finally { setCargando(false); }
  }, []);
  useEffect(() => { cargar(); }, [cargar]);

  async function crear(e) {
    e.preventDefault();
    if (!numero.trim()) return;
    setGuardando(true); setError(''); setMensaje('');
    try {
      await api('/mesas', { metodo: 'POST', cuerpo: { numero: numero.trim() } });
      setNumero(''); setModalNueva(false); setMensaje('Mesa registrada correctamente.'); await cargar();
    } catch (e) { setError(e.message || 'No se pudo registrar la mesa.'); }
    finally { setGuardando(false); }
  }
  async function cambiarActiva(mesa) {
    setError(''); setMensaje('');
    try { await api(`/mesas/${mesa.id}/activa`, { metodo: 'PATCH', cuerpo: { activa: !mesa.activa } }); setMensaje(`Mesa ${mesa.numero} ${mesa.activa ? 'desactivada' : 'activada'} correctamente.`); await cargar(); }
    catch (e) { setError(e.message || 'No se pudo actualizar la mesa.'); }
  }
  async function verHistorial(mesa) {
    setHistorial({ mesa, pedidos: null, cargando: true });
    try { const pedidos = await api(`/mesas/${mesa.id}/historial`); setHistorial({ mesa, pedidos: pedidos || [], cargando: false }); }
    catch (e) { setHistorial({ mesa, pedidos: [], cargando: false, error: e.message }); }
  }

  const libres = mesas.filter(m => m.activa && m.estado === 'LIBRE').length;
  const ocupadas = mesas.filter(m => m.activa && m.estado === 'OCUPADA').length;
  const inactivas = mesas.filter(m => !m.activa).length;
  const filtradas = useMemo(() => mesas.filter(m => {
    const q = busqueda.trim().toLowerCase();
    const coincide = !q || String(m.numero || '').toLowerCase().includes(q) || String(m.mozo || '').toLowerCase().includes(q);
    const estado = filtro === 'TODAS' || (filtro === 'DISPONIBLES' && m.activa && m.estado === 'LIBRE') || (filtro === 'OCUPADAS' && m.activa && m.estado === 'OCUPADA') || (filtro === 'INACTIVAS' && !m.activa);
    return coincide && estado;
  }), [mesas, busqueda, filtro]);

  const estadoMesa = mesa => !mesa.activa ? 'INACTIVA' : mesa.estado === 'OCUPADA' ? 'OCUPADA' : 'LIBRE';
  return <section className="mesas-gerente">
    <div className="fila-titulo mg-titulo"><div><h2>Mesas</h2><p>Organiza el salón y controla la disponibilidad de cada mesa.</p></div><div className="mg-titulo-acciones"><button className="boton secundario" onClick={cargar} disabled={cargando}><RefreshCw size={16}/> Actualizar</button><button className="boton" onClick={() => { setNumero(''); setError(''); setModalNueva(true); }}><Plus size={17}/> Nueva mesa</button></div></div>
    {error && <div className="error" role="alert">{error}</div>}{mensaje && <div className="mg-alerta-ok" role="status">{mensaje}</div>}
    <div className="mg-kpis">
      <article className="tarjeta mg-kpi"><span className="mg-kpi-icon mg-cafe"><Armchair/></span><div><small>Total de mesas</small><strong>{mesas.length}</strong><span>Registradas en el salón</span></div></article>
      <article className="tarjeta mg-kpi"><span className="mg-kpi-icon mg-verde"><CircleCheck/></span><div><small>Disponibles</small><strong>{libres}</strong><span>Listas para nuevos pedidos</span></div></article>
      <article className="tarjeta mg-kpi"><span className="mg-kpi-icon mg-naranja"><CircleAlert/></span><div><small>Ocupadas</small><strong>{ocupadas}</strong><span>Con pedidos sin cobrar</span></div></article>
      <article className="tarjeta mg-kpi"><span className="mg-kpi-icon mg-gris"><CircleOff/></span><div><small>Inactivas</small><strong>{inactivas}</strong><span>Fuera de servicio</span></div></article>
    </div>
    <div className="tarjeta mg-filtros"><label className="mg-busqueda"><Search size={17}/><input aria-label="Buscar mesa" value={busqueda} onChange={e => setBusqueda(e.target.value)} placeholder="Buscar por mesa o mozo…"/></label><div className="mg-filtro"><label htmlFor="mesa-filtro">Estado</label><select id="mesa-filtro" value={filtro} onChange={e => setFiltro(e.target.value)}><option value="TODAS">Todos los estados</option><option value="DISPONIBLES">Disponibles</option><option value="OCUPADAS">Ocupadas</option><option value="INACTIVAS">Inactivas</option></select></div></div>
    <div className="mg-listado-heading"><div><h3>Distribución de mesas</h3><p>Selecciona una mesa para consultar su actividad o administrar su estado.</p></div><span className="mg-contador">{filtradas.length} mesa(s)</span></div>
    <div className="mg-grid-mesas">
      {cargando ? <div className="tarjeta mg-vacio">Cargando mesas…</div> : filtradas.length === 0 ? <div className="tarjeta mg-vacio"><Armchair size={28}/><strong>No hay mesas para mostrar</strong><span>Prueba otro filtro o registra una nueva mesa.</span></div> : filtradas.map(mesa => {
        const estado = estadoMesa(mesa); const ocupada = estado === 'OCUPADA';
        return <article className={`tarjeta mg-mesa ${estado.toLowerCase()}`} key={mesa.id}>
          <div className="mg-mesa-top"><div className="mg-mesa-icon"><Armchair size={23}/></div><span className={`mg-estado ${estado.toLowerCase()}`}>{estado === 'LIBRE' ? 'Disponible' : estado === 'OCUPADA' ? 'Ocupada' : 'Inactiva'}</span></div>
          <div className="mg-mesa-nombre">Mesa <strong>{mesa.numero}</strong></div><div className="mg-mesa-sub">{ocupada ? `${mesa.pedidosAbiertos || 0} pedido(s) abierto(s)` : mesa.activa ? 'Lista para recibir clientes' : 'Deshabilitada temporalmente'}</div>
          {ocupada ? <div className="mg-ocupacion"><div><Clock3 size={15}/><span>Tiempo ocupada</span><strong>{mesa.minutosOcupada ?? 0} min</strong></div><div><Users size={15}/><span>Atiende</span><strong>{mesa.mozo || '—'}</strong></div><div className="mg-total"><ClipboardList size={15}/><span>Total abierto</span><strong>{dinero(mesa.totalAbierto)}</strong></div></div> : <div className="mg-mesa-info"><span>{mesa.activa ? '✓' : '–'}</span>{mesa.activa ? 'Disponible para el mozo' : 'No se asignará a nuevos pedidos'}</div>}
          <div className="mg-mesa-acciones"><button className="boton secundario" onClick={() => verHistorial(mesa)}><History size={15}/> Historial</button><button className={`boton ${mesa.activa ? 'secundario mg-desactivar' : ''}`} onClick={() => cambiarActiva(mesa)}><Power size={15}/>{mesa.activa ? 'Desactivar' : 'Activar'}</button></div>
        </article>;
      })}
    </div>
    {modalNueva && <div className="fondo-modal" role="presentation" onMouseDown={e => { if (e.target === e.currentTarget) setModalNueva(false); }}><div className="modal estrecho" role="dialog" aria-modal="true" aria-labelledby="mg-nueva-titulo"><div className="modal-cabecera"><h3 id="mg-nueva-titulo">Registrar nueva mesa</h3><button className="boton-icono" onClick={() => setModalNueva(false)} aria-label="Cerrar"><X/></button></div><p className="nota">Agrega el número o nombre con el que el personal identificará esta mesa.</p><form onSubmit={crear} className="mg-form-nueva"><label htmlFor="mg-numero">Número o nombre de mesa</label><input id="mg-numero" value={numero} onChange={e => setNumero(e.target.value)} maxLength={10} placeholder="Ej. 01 o Terraza 1" required autoFocus/><div className="mg-modal-acciones"><button type="button" className="boton secundario" onClick={() => setModalNueva(false)}>Cancelar</button><button type="submit" className="boton" disabled={guardando}><Plus size={16}/>{guardando ? 'Guardando…' : 'Registrar mesa'}</button></div></form></div></div>}
    {historial && <div className="fondo-modal" role="presentation" onMouseDown={e => { if (e.target === e.currentTarget) setHistorial(null); }}><div className="modal" role="dialog" aria-modal="true" aria-labelledby="mg-historial-titulo"><div className="modal-cabecera"><h3 id="mg-historial-titulo">Historial · Mesa {historial.mesa.numero}</h3><button className="boton-icono" onClick={() => setHistorial(null)} aria-label="Cerrar"><X/></button></div>{historial.cargando ? <p>Cargando historial…</p> : historial.error ? <div className="error">{historial.error}</div> : historial.pedidos.length === 0 ? <p className="vacio">Esta mesa todavía no tiene pedidos en su historial.</p> : <div className="contenedor-tabla"><table className="tabla"><thead><tr><th>Pedido</th><th>Fecha</th><th>Estado</th><th>Total</th></tr></thead><tbody>{historial.pedidos.map((p, i) => <tr key={p.id || p.codigo || i}><td>{p.codigo || p.numero || '—'}</td><td>{fecha(p.fechaRegistro || p.fechaHora || p.creadoEn)}</td><td>{p.estado || '—'}</td><td>{dinero(p.total)}</td></tr>)}</tbody></table></div>}</div></div>}
  </section>;
}
