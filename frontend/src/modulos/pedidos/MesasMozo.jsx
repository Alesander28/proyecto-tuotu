import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { Armchair, ArrowRight, Clock3, ClipboardList, RefreshCw, Search, ShoppingBag, Users, X } from 'lucide-react';
import { api } from '../../comun/api.js';
import { soles } from '../../comun/formato.js';
import './mesas-mozo.css';
import './mozo-redisenio.css';

const duracion = (minutos) => {
  if (minutos == null) return '—';
  const n = Math.max(0, Number(minutos));
  return n >= 60 ? `${Math.floor(n / 60)} h ${n % 60} min` : `${n} min`;
};

export default function MesasMozo() {
  const [mesas, setMesas] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [actualizando, setActualizando] = useState(false);
  const [error, setError] = useState('');
  const [filtro, setFiltro] = useState('TODAS');
  const [busqueda, setBusqueda] = useState('');
  const [historialMesa, setHistorialMesa] = useState(null);
  const [historial, setHistorial] = useState([]);
  const [cargandoHistorial, setCargandoHistorial] = useState(false);
  const [errorHistorial, setErrorHistorial] = useState('');

  const cargar = useCallback(async (silencioso = false) => {
    if (!silencioso) setActualizando(true);
    try {
      const datos = await api('/mesas');
      setMesas(Array.isArray(datos) ? datos : []);
      setError('');
    } catch (e) {
      setError(e.message || 'No se pudieron consultar las mesas.');
    } finally {
      setCargando(false);
      setActualizando(false);
    }
  }, []);

  useEffect(() => {
    cargar();
    const reloj = setInterval(() => cargar(true), 15000);
    return () => clearInterval(reloj);
  }, [cargar]);

  const resumen = useMemo(() => ({
    activas: mesas.filter((m) => m.activa).length,
    libres: mesas.filter((m) => m.activa && m.estado === 'LIBRE').length,
    ocupadas: mesas.filter((m) => m.activa && m.estado === 'OCUPADA').length,
    inactivas: mesas.filter((m) => !m.activa).length,
  }), [mesas]);

  const visibles = useMemo(() => mesas
    .filter((m) => filtro === 'TODAS' || (!m.activa ? filtro === 'INACTIVA' : m.estado === filtro))
    .filter((m) => String(m.numero).toLowerCase().includes(busqueda.trim().toLowerCase()))
    .sort((a, b) => String(a.numero).localeCompare(String(b.numero), 'es', { numeric: true })), [mesas, filtro, busqueda]);

  async function verHistorial(mesa) {
    setHistorialMesa(mesa);
    setHistorial([]);
    setErrorHistorial('');
    setCargandoHistorial(true);
    try {
      const datos = await api(`/mesas/${mesa.id}/historial`);
      setHistorial(Array.isArray(datos) ? datos : []);
    } catch (e) {
      setErrorHistorial(e.message || 'No se pudo cargar el historial.');
    } finally {
      setCargandoHistorial(false);
    }
  }

  return <div className="mesas-mozo">
    <div className="fila-titulo mesas-titulo">
      <div><span className="mesas-eyebrow">ÁREA DE ATENCIÓN</span><h2>Gestión de mesas</h2><p>Consulta la disponibilidad del salón y registra pedidos en la mesa correspondiente.</p></div>
      <button type="button" className="boton secundario" onClick={() => cargar()} disabled={actualizando}><RefreshCw size={16} className={actualizando ? 'mesas-girando' : ''} />{actualizando ? 'Actualizando…' : 'Actualizar'}</button>
    </div>
    {error && <div className="error" role="alert">{error}</div>}

    <div className="mesas-resumen">
      <article className="tarjeta mesas-kpi"><span className="mesas-kpi-icono"><Armchair size={21} /></span><div><small>Mesas activas</small><strong>{cargando ? '—' : resumen.activas}</strong></div></article>
      <article className="tarjeta mesas-kpi libre"><span className="mesas-kpi-icono"><i className="mesas-punto" /></span><div><small>Disponibles</small><strong>{cargando ? '—' : resumen.libres}</strong></div></article>
      <article className="tarjeta mesas-kpi ocupada"><span className="mesas-kpi-icono"><Users size={21} /></span><div><small>Ocupadas</small><strong>{cargando ? '—' : resumen.ocupadas}</strong></div></article>
      <article className="tarjeta mesas-kpi inactiva"><span className="mesas-kpi-icono"><Clock3 size={21} /></span><div><small>Fuera de servicio</small><strong>{cargando ? '—' : resumen.inactivas}</strong></div></article>
    </div>

    <section className="tarjeta mesas-panel">
      <div className="mesas-panel-cabecera"><div><h3>Estado del salón</h3><p>Una mesa permanece ocupada mientras tenga pedidos sin cobrar.</p></div><div className="mesas-leyenda"><span><i className="libre" /> Libre</span><span><i className="ocupada" /> Ocupada</span><span><i className="inactiva" /> Inactiva</span></div></div>
      <div className="mesas-filtros">
        <div className="mesas-buscador"><Search size={17} /><input aria-label="Buscar mesa" placeholder="Buscar mesa…" value={busqueda} onChange={(e) => setBusqueda(e.target.value)} /></div>
        <div className="conmutador" aria-label="Filtrar mesas">
          {[['TODAS', 'Todas'], ['LIBRE', 'Libres'], ['OCUPADA', 'Ocupadas'], ['INACTIVA', 'Inactivas']].map(([id, nombre]) => <button key={id} type="button" aria-pressed={filtro === id} onClick={() => setFiltro(id)}>{nombre}</button>)}
        </div>
      </div>
      {cargando ? <div className="vacio">Cargando disponibilidad de las mesas…</div> : visibles.length === 0 ? <div className="mesas-vacio"><Armchair size={34} /><strong>{mesas.length === 0 ? 'Todavía no hay mesas registradas' : 'No se encontraron mesas'}</strong><p>{mesas.length === 0 ? 'Solicita al gerente que registre y active las mesas para comenzar a tomar pedidos.' : 'Prueba con otro filtro o cambia el texto de búsqueda.'}</p>{mesas.length === 0 && <span className="mesas-nota-rol">El registro y la activación de mesas corresponden al gerente.</span>}</div> : (
        <div className="mesas-grid">{visibles.map((mesa) => {
          const estado = !mesa.activa ? 'INACTIVA' : mesa.estado;
          const ocupada = estado === 'OCUPADA';
          return <article key={mesa.id} className={`mesa-card ${estado.toLowerCase()}`}>
            <div className="mesa-card-top"><span className="mesa-card-icono"><Armchair size={23} /></span><span className={`mesa-estado ${estado.toLowerCase()}`}>{estado === 'LIBRE' ? 'Disponible' : ocupada ? 'Ocupada' : 'Inactiva'}</span></div>
            <div className="mesa-card-numero">Mesa {mesa.numero}</div>
            {ocupada ? <div className="mesa-card-detalle"><span><ClipboardList size={14} /> {mesa.pedidosAbiertos} pedido(s) abierto(s)</span><span><Clock3 size={14} /> Ocupada hace {duracion(mesa.minutosOcupada)}</span><span><Users size={14} /> Atiende: {mesa.mozo || 'No disponible'}</span><strong className="mesa-total">{soles(mesa.totalAbierto || 0)}</strong></div> : <p className="mesa-card-disponible">{mesa.activa ? 'Lista para recibir clientes y nuevos pedidos.' : 'Esta mesa está deshabilitada por el gerente.'}</p>}
            <div className="mesa-card-acciones">{mesa.activa ? <Link className="boton mesa-boton-pedido" to={`/pedidos/nuevo?mesa=${encodeURIComponent(mesa.numero)}`}><ShoppingBag size={16} />{ocupada ? 'Agregar pedido' : 'Crear pedido'}<ArrowRight size={15} /></Link> : <button type="button" className="boton secundario" disabled>Mesa no disponible</button>}{ocupada && <button type="button" className="mesa-link-historial" onClick={() => verHistorial(mesa)}>Ver historial</button>}</div>
          </article>;
        })}</div>
      )}
      <p className="mesas-actualizacion"><RefreshCw size={13} /> La disponibilidad se actualiza automáticamente cada 15 segundos.</p>
    </section>

    {historialMesa && <div className="fondo-modal" role="presentation" onMouseDown={(e) => { if (e.target === e.currentTarget) setHistorialMesa(null); }}><section className="modal mesas-modal" role="dialog" aria-modal="true" aria-labelledby="historial-titulo"><div className="modal-cabecera"><div><h3 id="historial-titulo">Historial · Mesa {historialMesa.numero}</h3><p>Pedidos registrados en esta mesa.</p></div><button type="button" className="boton-icono" aria-label="Cerrar historial" onClick={() => setHistorialMesa(null)}><X size={19} /></button></div>{errorHistorial && <p className="error" role="alert">{errorHistorial}</p>}{cargandoHistorial ? <p className="nota">Cargando historial…</p> : historial.length === 0 ? <div className="vacio">No hay pedidos anteriores para esta mesa.</div> : <div className="contenedor-tabla"><table className="tabla"><thead><tr><th>Pedido</th><th>Fecha y hora</th><th>Mozo</th><th>Estado</th><th className="num">Total</th></tr></thead><tbody>{historial.map((p) => <tr key={p.id}><td><strong>{p.codigo}</strong></td><td>{p.fechaHora ? new Date(p.fechaHora).toLocaleString('es-PE') : '—'}</td><td>{p.mozo || '—'}</td><td>{p.estado}</td><td className="num">{soles(p.total)}</td></tr>)}</tbody></table></div>}</section></div>}
  </div>;
}
