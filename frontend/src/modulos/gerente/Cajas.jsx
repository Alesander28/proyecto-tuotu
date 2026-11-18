import { useCallback, useEffect, useMemo, useState } from 'react';
import { Banknote, CalendarDays, CheckCircle2, CircleDollarSign, Clock3, RefreshCw, Search, TrendingUp, Wallet } from 'lucide-react';
import { api } from '../../comun/api.js';
import './cajas-gerente.css';

const dinero = (v) => `S/ ${Number(v || 0).toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
const fecha = (v) => v ? new Date(v).toLocaleString('es-PE', { dateStyle: 'short', timeStyle: 'short' }) : '—';
const hoy = () => new Date().toLocaleDateString('en-CA');
const inicioSemana = () => { const d = new Date(); d.setDate(d.getDate() - ((d.getDay() + 6) % 7)); return d.toLocaleDateString('en-CA'); };
const inicioMes = () => { const d = new Date(); d.setDate(1); return d.toLocaleDateString('en-CA'); };

export default function Cajas() {
  const [cajas, setCajas] = useState([]);
  const [desde, setDesde] = useState(hoy());
  const [hasta, setHasta] = useState(hoy());
  const [estado, setEstado] = useState('');
  const [busqueda, setBusqueda] = useState('');
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState('');
  const [periodo, setPeriodo] = useState('hoy');

  const cargar = useCallback(async () => {
    setCargando(true); setError('');
    const params = new URLSearchParams();
    if (estado) params.set('estado', estado);
    if (desde) params.set('desde', desde);
    if (hasta) params.set('hasta', hasta);
    try { setCajas(await api(`/cajas?${params.toString()}`) || []); }
    catch (e) { setError(e.message || 'No se pudieron consultar las cajas.'); }
    finally { setCargando(false); }
  }, [estado, desde, hasta]);
  useEffect(() => { cargar(); }, [cargar]);

  const visibles = useMemo(() => cajas.filter(c => `${c.cajero || ''} ${c.cajeroUsuario || ''}`.toLowerCase().includes(busqueda.toLowerCase())), [cajas, busqueda]);
  const abiertas = cajas.filter(c => c.estado === 'ABIERTA');
  const ventas = cajas.reduce((s, c) => s + Number(c.totalVentas || 0), 0);
  const efectivo = cajas.reduce((s, c) => s + Number(c.ventasEfectivo || 0), 0);
  const diferencias = cajas.filter(c => c.estado === 'CERRADA' && Number(c.diferencia || 0) !== 0);
  const sumaDiferencias = cajas.reduce((s, c) => s + Number(c.diferencia || 0), 0);

  function seleccionarPeriodo(p) {
    setPeriodo(p);
    const fin = hoy();
    if (p === 'hoy') { setDesde(fin); setHasta(fin); }
    if (p === 'semana') { setDesde(inicioSemana()); setHasta(fin); }
    if (p === 'mes') { setDesde(inicioMes()); setHasta(fin); }
    if (p === 'todo') { setDesde(''); setHasta(''); }
  }
  const formatoEstado = c => c.estado === 'ABIERTA' ? 'Abierta' : 'Cerrada';

  return <section className="cajas-gerente">
    <header className="cajas-hero"><div className="cajas-hero-icon"><Wallet size={29}/></div><div className="cajas-hero-text"><h2>Control de cajas</h2><p>Supervisa turnos, ingresos y cierres de caja de tu equipo.</p></div><button className="boton cajas-refresh" onClick={cargar} disabled={cargando}><RefreshCw size={16} className={cargando ? 'cajas-girando' : ''}/> Actualizar</button></header>
    {error && <div className="error" role="alert">{error}</div>}
    <div className="cajas-kpis">
      <article className="cajas-kpi"><div className="cajas-kpi-top"><span className="cajas-kpi-icon cafe"><Wallet size={19}/></span><span className="cajas-kpi-tag">Periodo seleccionado</span></div><small>Cajas consultadas</small><strong>{cajas.length}</strong><p><CalendarDays size={13}/> Turnos registrados</p></article>
      <article className="cajas-kpi"><div className="cajas-kpi-top"><span className="cajas-kpi-icon verde"><Clock3 size={19}/></span><span className="cajas-kpi-tag activo">En operación</span></div><small>Cajas abiertas</small><strong>{abiertas.length}</strong><p><CheckCircle2 size={13}/> Turnos activos</p></article>
      <article className="cajas-kpi"><div className="cajas-kpi-top"><span className="cajas-kpi-icon naranja"><TrendingUp size={19}/></span><span className="cajas-kpi-tag">Ventas registradas</span></div><small>Total vendido</small><strong>{dinero(ventas)}</strong><p><CircleDollarSign size={13}/> Todos los métodos de pago</p></article>
      <article className="cajas-kpi"><div className="cajas-kpi-top"><span className="cajas-kpi-icon ambar"><Banknote size={19}/></span><span className={`cajas-kpi-tag ${diferencias.length ? 'alerta' : 'activo'}`}>{diferencias.length ? 'Revisar' : 'Sin diferencias'}</span></div><small>Diferencias de cierre</small><strong>{diferencias.length}</strong><p className={sumaDiferencias < 0 ? 'negativo' : ''}>Saldo acumulado: {dinero(sumaDiferencias)}</p></article>
    </div>
    <div className="cajas-resumen-mini"><div><span>Ventas en efectivo</span><strong>{dinero(efectivo)}</strong></div><div><span>Turnos abiertos</span><strong>{abiertas.length}</strong></div><div><span>Cierres con diferencia</span><strong>{diferencias.length}</strong></div></div>
    <section className="cajas-contenedor"><div className="cajas-seccion-cab"><div><h3>Historial de cajas</h3><p>Consulta aperturas, cierres y arqueos registrados.</p></div><span className="cajas-total">{visibles.length} registro(s)</span></div>
      <div className="cajas-filtros"><div className="cajas-periodos" role="group" aria-label="Periodo"><button className={periodo==='hoy'?'seleccionado':''} onClick={()=>seleccionarPeriodo('hoy')}>Hoy</button><button className={periodo==='semana'?'seleccionado':''} onClick={()=>seleccionarPeriodo('semana')}>Esta semana</button><button className={periodo==='mes'?'seleccionado':''} onClick={()=>seleccionarPeriodo('mes')}>Este mes</button><button className={periodo==='todo'?'seleccionado':''} onClick={()=>seleccionarPeriodo('todo')}>Todo</button></div>
        <div className="cajas-busqueda"><Search size={16}/><input value={busqueda} onChange={e=>setBusqueda(e.target.value)} placeholder="Buscar cajero…" aria-label="Buscar cajero"/></div>
        <div className="cajas-fechas"><label>Desde<input type="date" value={desde} onChange={e=>{setDesde(e.target.value);setPeriodo('custom')}}/></label><label>Hasta<input type="date" value={hasta} onChange={e=>{setHasta(e.target.value);setPeriodo('custom')}}/></label><label>Estado<select value={estado} onChange={e=>setEstado(e.target.value)}><option value="">Todos</option><option value="ABIERTA">Abierta</option><option value="CERRADA">Cerrada</option></select></label><button className="boton" onClick={cargar} disabled={cargando}><Search size={15}/> Filtrar</button></div>
      </div>
      {cargando ? <div className="cajas-empty">Cargando información de cajas…</div> : visibles.length === 0 ? <div className="cajas-empty"><div className="cajas-empty-icon"><Wallet size={25}/></div><strong>No hay cajas en este periodo</strong><p>Prueba otro rango de fechas o cambia los filtros.</p></div> : <div className="cajas-tabla-wrap"><table className="cajas-tabla"><thead><tr><th>Cajero</th><th>Apertura</th><th>Cierre</th><th>Estado</th><th>Ventas</th><th>Total vendido</th><th>Efectivo esperado</th><th>Contado</th><th>Diferencia</th></tr></thead><tbody>{visibles.map(c=><tr key={c.id}><td><div className="cajas-cajero"><span>{(c.cajero||c.cajeroUsuario||'?').trim().charAt(0).toUpperCase()}</span><div><strong>{c.cajero||c.cajeroUsuario||'—'}</strong>{c.cajeroUsuario&&c.cajero&&<small>{c.cajeroUsuario}</small>}</div></div></td><td>{fecha(c.fechaApertura)}</td><td>{fecha(c.fechaCierre)}</td><td><span className={`cajas-estado ${c.estado==='ABIERTA'?'abierta':'cerrada'}`}><i/>{formatoEstado(c)}</span></td><td>{c.cantidadVentas??0}</td><td><strong>{dinero(c.totalVentas)}</strong></td><td>{dinero(c.efectivoEsperado)}</td><td>{c.montoContado==null?'—':dinero(c.montoContado)}</td><td>{c.diferencia==null?'—':<strong className={`cajas-dif ${Number(c.diferencia)<0?'falta':Number(c.diferencia)>0?'sobra':'cuadra'}`}>{dinero(c.diferencia)}</strong>}</td></tr>)}</tbody></table></div>}
    </section>
    <div className="cajas-nota"><Banknote size={16}/><span>Los montos y diferencias se muestran según los registros de apertura, ventas y cierre de cada cajero. Esta vista es de consulta para el Gerente.</span></div>
  </section>;
}
