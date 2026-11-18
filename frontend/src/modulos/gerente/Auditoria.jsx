import { useCallback, useEffect, useMemo, useState } from 'react';
import { Activity, CalendarDays, ClipboardList, Download, Eye, FileSearch, RefreshCw, Search, ShieldCheck, Users, X } from 'lucide-react';
import { api } from '../../comun/api.js';
import './auditoria-gerente.css';

const fecha = (v) => v ? new Date(v).toLocaleString('es-PE', { dateStyle: 'medium', timeStyle: 'short' }) : '—';
const hoy = () => new Date().toLocaleDateString('en-CA');
const inicioSemana = () => { const d = new Date(); d.setDate(d.getDate() - ((d.getDay() + 6) % 7)); return d.toLocaleDateString('en-CA'); };
const inicioMes = () => { const d = new Date(); d.setDate(1); return d.toLocaleDateString('en-CA'); };
const csvCell = (v) => `"${String(v ?? '').replaceAll('"', '""')}"`;

export default function Auditoria() {
  const [registros, setRegistros] = useState([]);
  const [modulo, setModulo] = useState('');
  const [usuario, setUsuario] = useState('');
  const [q, setQ] = useState('');
  const [desde, setDesde] = useState('');
  const [hasta, setHasta] = useState('');
  const [periodo, setPeriodo] = useState('30dias');
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState('');
  const [detalle, setDetalle] = useState(null);

  const cargar = useCallback(async () => {
    setCargando(true); setError('');
    const params = new URLSearchParams();
    if (modulo) params.set('modulo', modulo);
    if (usuario.trim()) params.set('usuario', usuario.trim());
    if (q.trim()) params.set('q', q.trim());
    if (desde) params.set('desde', desde);
    if (hasta) params.set('hasta', hasta);
    try { setRegistros(await api(`/auditoria?${params.toString()}`) || []); }
    catch (e) { setError(e.message || 'No se pudo consultar la auditoría.'); setRegistros([]); }
    finally { setCargando(false); }
  }, [modulo, usuario, q, desde, hasta]);
  useEffect(() => { cargar(); }, [cargar]);

  const usuarios = useMemo(() => new Set(registros.map(r => r.usuario).filter(Boolean)).size, [registros]);
  const modulos = useMemo(() => [...new Set(registros.map(r => r.modulo).filter(Boolean))].sort(), [registros]);
  const tipos = useMemo(() => [...new Set(registros.map(r => r.tipo).filter(Boolean))], [registros]);

  function elegirPeriodo(p) {
    setPeriodo(p);
    const fin = hoy();
    if (p === 'hoy') { setDesde(fin); setHasta(fin); }
    if (p === 'semana') { setDesde(inicioSemana()); setHasta(fin); }
    if (p === 'mes') { setDesde(inicioMes()); setHasta(fin); }
    if (p === '30dias') { setDesde(''); setHasta(''); }
  }
  function exportar() {
    const filas = [['ID', 'Fecha y hora', 'Usuario', 'Módulo', 'Acción', 'Descripción', 'Referencia'], ...registros.map(r => [r.id, r.fechaHora, r.usuario, r.modulo, r.tipo, r.descripcion, r.referencia])];
    const contenido = '\uFEFF' + filas.map(f => f.map(csvCell).join(';')).join('\r\n');
    const blob = new Blob([contenido], { type: 'text/csv;charset=utf-8' });
    const url = URL.createObjectURL(blob); const a = document.createElement('a');
    a.href = url; a.download = `auditoria-tuotu-${hoy()}.csv`; a.click(); URL.revokeObjectURL(url);
  }

  return <section className="auditoria-gerente">
    <header className="audit-hero"><div className="audit-hero-icon"><ShieldCheck size={27}/></div><div className="audit-hero-copy"><h2>Auditoría del sistema</h2><p>Supervisa las operaciones y consulta el historial de actividad de los usuarios.</p></div><div className="audit-hero-actions"><button className="boton secundario" onClick={cargar} disabled={cargando}><RefreshCw size={16} className={cargando ? 'audit-spin' : ''}/> Actualizar</button><button className="boton" onClick={exportar} disabled={cargando || registros.length===0}><Download size={16}/> Exportar CSV</button></div></header>
    {error && <div className="error" role="alert">{error}</div>}
    <div className="audit-kpis">
      <article className="audit-kpi"><div className="audit-kpi-top"><span className="audit-icon brown"><ClipboardList size={18}/></span><span className="audit-pill">Filtrado</span></div><small>Operaciones encontradas</small><strong>{registros.length}</strong><p>Eventos según los filtros actuales</p></article>
      <article className="audit-kpi"><div className="audit-kpi-top"><span className="audit-icon orange"><Activity size={18}/></span><span className="audit-pill">Actividad</span></div><small>Módulos con registros</small><strong>{modulos.length}</strong><p>{modulos.length ? modulos.join(' · ') : 'Sin actividad en este periodo'}</p></article>
      <article className="audit-kpi"><div className="audit-kpi-top"><span className="audit-icon green"><Users size={18}/></span><span className="audit-pill">Usuarios</span></div><small>Usuarios activos en el historial</small><strong>{usuarios}</strong><p>Usuarios que aparecen en los resultados</p></article>
      <article className="audit-kpi"><div className="audit-kpi-top"><span className="audit-icon gold"><FileSearch size={18}/></span><span className="audit-pill">Tipos</span></div><small>Acciones diferentes</small><strong>{tipos.length}</strong><p>{tipos.length ? tipos.join(' · ') : 'No se encontraron acciones'}</p></article>
    </div>
    <section className="audit-panel"><div className="audit-panel-heading"><div><h3>Historial de operaciones</h3><p>Registros de solo lectura: no se pueden editar ni eliminar.</p></div><span className="audit-count">{registros.length} registro(s)</span></div>
      <div className="audit-filters"><div className="audit-periods" role="group" aria-label="Periodo"><button className={periodo==='hoy'?'active':''} onClick={()=>elegirPeriodo('hoy')}>Hoy</button><button className={periodo==='semana'?'active':''} onClick={()=>elegirPeriodo('semana')}>Esta semana</button><button className={periodo==='mes'?'active':''} onClick={()=>elegirPeriodo('mes')}>Este mes</button><button className={periodo==='30dias'?'active':''} onClick={()=>elegirPeriodo('30dias')}>Últimos 30 días</button></div>
        <div className="audit-search"><Search size={16}/><input value={q} onChange={e=>setQ(e.target.value)} placeholder="Buscar descripción o referencia…" aria-label="Buscar descripción o referencia"/></div>
        <div className="audit-filter-row"><label>Usuario<input value={usuario} onChange={e=>setUsuario(e.target.value)} placeholder="Buscar usuario"/></label><label>Módulo<select value={modulo} onChange={e=>setModulo(e.target.value)}><option value="">Todos los módulos</option>{['Seguridad','Pedidos','Productos','Inventario','Ventas','Cajas','Mesas'].map(m=><option key={m} value={m}>{m}</option>)}</select></label><label><span><CalendarDays size={13}/> Desde</span><input type="date" value={desde} onChange={e=>{setDesde(e.target.value);setPeriodo('personalizado')}}/></label><label><span><CalendarDays size={13}/> Hasta</span><input type="date" value={hasta} onChange={e=>{setHasta(e.target.value);setPeriodo('personalizado')}}/></label><button className="boton audit-filter-button" onClick={cargar} disabled={cargando}><Search size={15}/> Aplicar</button></div>
      </div>
      {cargando ? <div className="audit-empty"><RefreshCw size={23} className="audit-spin"/><span>Consultando registros…</span></div> : registros.length===0 ? <div className="audit-empty"><div className="audit-empty-icon"><FileSearch size={25}/></div><strong>No hay actividad que mostrar</strong><p>Prueba cambiar las fechas o quitar algún filtro.</p></div> : <div className="audit-table-wrap"><table className="audit-table"><thead><tr><th>Fecha y hora</th><th>Usuario</th><th>Módulo</th><th>Acción</th><th>Descripción</th><th>Referencia</th><th></th></tr></thead><tbody>{registros.map(r=><tr key={r.id}><td className="audit-date">{fecha(r.fechaHora)}</td><td><div className="audit-user"><span>{(r.usuario||'?').trim().charAt(0).toUpperCase()}</span><strong>{r.usuario||'—'}</strong></div></td><td><span className="audit-module">{r.modulo||'—'}</span></td><td><span className="audit-action">{r.tipo||'—'}</span></td><td className="audit-description">{r.descripcion||'—'}</td><td>{r.referencia||'—'}</td><td><button className="audit-detail-btn" onClick={()=>setDetalle(r)} title="Ver detalle" aria-label={`Ver detalle del registro ${r.id}`}><Eye size={16}/></button></td></tr>)}</tbody></table></div>}
    </section>
    <div className="audit-readonly-note"><ShieldCheck size={16}/><span>Auditoría es un historial de consulta. Los registros se conservan como evidencia de las operaciones realizadas en el sistema.</span></div>
    {detalle && <div className="audit-modal-backdrop" onClick={()=>setDetalle(null)}><div className="audit-modal" role="dialog" aria-modal="true" aria-labelledby="audit-detail-title" onClick={e=>e.stopPropagation()}><header><div><span className="audit-modal-label">Registro #{detalle.id}</span><h3 id="audit-detail-title">Detalle de operación</h3></div><button onClick={()=>setDetalle(null)} className="audit-close" aria-label="Cerrar"><X size={19}/></button></header><div className="audit-detail-grid"><div><small>Fecha y hora</small><strong>{fecha(detalle.fechaHora)}</strong></div><div><small>Usuario</small><strong>{detalle.usuario||'—'}</strong></div><div><small>Módulo</small><strong>{detalle.modulo||'—'}</strong></div><div><small>Acción</small><strong>{detalle.tipo||'—'}</strong></div><div className="wide"><small>Descripción</small><strong>{detalle.descripcion||'—'}</strong></div><div className="wide"><small>Referencia</small><strong>{detalle.referencia||'—'}</strong></div></div><footer><button className="boton secundario" onClick={()=>setDetalle(null)}>Cerrar</button></footer></div></div>}
  </section>;
}
