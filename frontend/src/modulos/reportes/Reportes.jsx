import { useCallback, useEffect, useMemo, useState } from 'react';
import { api } from '../../comun/api.js';
import { soles } from '../../comun/formato.js';
import {
  Activity, CalendarDays, ChartNoAxesCombined, CircleDollarSign, ClipboardList, Download,
  FileDown, Package, RefreshCw, ShoppingBag, Trophy, Wallet,
} from 'lucide-react';
import {
  Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis,
  Pie, PieChart, Cell, Bar, BarChart,
} from 'recharts';
import './reportes.css';

const COLORES = ['#e87508', '#3b82d0', '#2e8b57', '#8b5bc4', '#c66b42', '#d6a21e'];
const fechaLocal = (fecha = new Date()) => {
  const d = new Date(fecha.getTime() - fecha.getTimezoneOffset() * 60000);
  return d.toISOString().slice(0, 10);
};
const inicioSemana = (f) => { const d = new Date(`${f}T12:00:00`); d.setDate(d.getDate() - ((d.getDay() + 6) % 7)); return fechaLocal(d); };
const inicioMes = (f) => `${f.slice(0, 7)}-01`;
const sumarDias = (f, n) => { const d = new Date(`${f}T12:00:00`); d.setDate(d.getDate() + n); return fechaLocal(d); };
const fechaCorta = (f) => { if (!f) return ''; const d = new Date(`${f}T12:00:00`); return d.toLocaleDateString('es-PE', { day: '2-digit', month: 'short' }); };
const porcentaje = (n, total) => total ? `${Math.round((Number(n) / Number(total)) * 100)}%` : '0%';
const csvCelda = (v) => `"${String(v ?? '').replaceAll('"', '""')}"`;

export default function Reportes() {
  const hoy = fechaLocal();
  const [desde, setDesde] = useState(inicioMes(hoy));
  const [hasta, setHasta] = useState(hoy);
  const [preset, setPreset] = useState('mes');
  const [ventas, setVentas] = useState(null);
  const [inventario, setInventario] = useState(null);
  const [serie, setSerie] = useState([]);
  const [pedidos, setPedidos] = useState(null);
  const [error, setError] = useState('');
  const [cargando, setCargando] = useState(false);
  const [agrupacion, setAgrupacion] = useState('dia');

  const consulta = useMemo(() => `?desde=${desde}&hasta=${hasta}`, [desde, hasta]);
  const generar = useCallback(async () => {
    setError(''); setCargando(true);
    try {
      const [v, i, s, p] = await Promise.all([
        api('/reportes/ventas' + consulta), api('/reportes/inventario' + consulta),
        api('/reportes/serie' + consulta + `&agrupar=${agrupacion}`), api('/reportes/pedidos' + consulta),
      ]);
      setVentas(v); setInventario(i); setSerie(Array.isArray(s) ? s : []); setPedidos(p);
    } catch (e) { setError(e.message); }
    finally { setCargando(false); }
  }, [consulta, agrupacion]);
  useEffect(() => { generar(); }, [generar]);

  const seleccionarPeriodo = (tipo) => {
    const f = fechaLocal(); setPreset(tipo);
    if (tipo === 'hoy') { setDesde(f); setHasta(f); }
    if (tipo === 'semana') { setDesde(inicioSemana(f)); setHasta(f); }
    if (tipo === 'mes') { setDesde(inicioMes(f)); setHasta(f); }
    if (tipo === 'personalizado') return;
  };
  const totalEntradas = inventario?.insumos?.reduce((s, i) => s + Number(i.entradas || 0), 0) || 0;
  const totalSalidas = inventario?.insumos?.reduce((s, i) => s + Number(i.salidas || 0), 0) || 0;
  const categorias = ventas?.porCategoria || [];
  const categoriasChart = categorias.map((c) => ({ name: c.nombre, value: Number(c.monto || 0) }));
  const serieChart = serie.map((p) => ({ fecha: fechaCorta(p.periodo), monto: Number(p.monto || 0), ventas: Number(p.ventas || 0) }));
  const masVendidos = ventas?.masVendidos || [];
  const maxProducto = Math.max(1, ...masVendidos.map((p) => Number(p.cantidad || 0)));
  const metodos = ventas?.porMetodoPago || [];
  const cajeros = ventas?.porCajero || [];

  const exportarCSV = () => {
    if (!ventas || !inventario) return;
    const filas = [['REPORTE TU O TU', `Periodo: ${desde} a ${hasta}`], [], ['RESUMEN', 'Valor'],
      ['Ventas cobradas', ventas.cantidadVentas], ['Ingresos totales', ventas.totalVendido], ['Ticket promedio', ventas.ticketPromedio],
      ['Pedidos registrados', pedidos?.total ?? ''], [], ['VENTAS POR CATEGORIA', 'Monto'],
      ...categorias.map((c) => [c.nombre, c.monto]), [], ['PRODUCTOS MAS VENDIDOS', 'Unidades'],
      ...masVendidos.map((p) => [p.nombre, p.cantidad]), [], ['METODOS DE PAGO', 'Monto'],
      ...metodos.map((m) => [m.nombre, m.monto]), [], ['CAJEROS', 'Monto vendido'],
      ...cajeros.map((c) => [c.nombre, c.monto]), [], ['INVENTARIO', 'Unidad', 'Entradas', 'Salidas', 'Stock actual'],
      ...inventario.insumos.map((i) => [i.insumo, i.unidad, i.entradas, i.salidas, i.stockActual])];
    const csv = filas.map((r) => r.map(csvCelda).join(';')).join('\r\n');
    const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8' });
    const url = URL.createObjectURL(blob); const a = document.createElement('a');
    a.href = url; a.download = `reporte-tuotu-${desde}-${hasta}.csv`; a.click(); URL.revokeObjectURL(url);
  };

  return (
    <main className="reportes-moderno">
      <header className="reportes-titulo fila-titulo">
        <div><h2>Reportes</h2><p>Analiza ventas, ingresos y rendimiento de tu restaurante.</p></div>
        <div className="reportes-acciones no-imprimir">
          <button className="boton" onClick={exportarCSV} disabled={!ventas || !inventario}><Download size={17}/> Exportar CSV</button>
          <button className="boton secundario" onClick={() => window.print()}><FileDown size={17}/> Descargar PDF</button>
        </div>
      </header>

      <section className="reportes-filtros tarjeta no-imprimir" aria-label="Filtros de fechas">
        <div className="reportes-presets">
          {['hoy', 'semana', 'mes', 'personalizado'].map((p) => <button key={p} className={preset === p ? 'activo' : ''} onClick={() => seleccionarPeriodo(p)}>
            {p === 'hoy' ? 'Hoy' : p === 'semana' ? 'Esta semana' : p === 'mes' ? 'Este mes' : <><CalendarDays size={15}/> Fechas</>}
          </button>)}
        </div>
        <div className="reportes-rango"><label>Desde<input type="date" value={desde} max={hasta} onChange={(e) => { setDesde(e.target.value); setPreset('personalizado'); }}/></label>
          <label>Hasta<input type="date" value={hasta} min={desde} max={hoy} onChange={(e) => { setHasta(e.target.value); setPreset('personalizado'); }}/></label>
          <button className="boton secundario reportes-actualizar" onClick={generar} disabled={cargando}><RefreshCw size={16} className={cargando ? 'reportes-girando' : ''}/> Actualizar</button>
        </div>
      </section>
      {error && <p className="error" role="alert">{error}</p>}
      {cargando && !ventas && <p className="reportes-cargando"><Activity size={18}/> Cargando reportes…</p>}

      {ventas && <>
        <section className="reportes-kpis" aria-label="Resumen de ventas">
          <article className="reportes-kpi tarjeta"><span className="reportes-kpi-icono naranja"><CircleDollarSign/></span><div><small>Ventas totales</small><strong data-testid="total-vendido">{soles(ventas.totalVendido)}</strong><span className="reportes-kpi-nota">Ingresos cobrados</span></div></article>
          <article className="reportes-kpi tarjeta"><span className="reportes-kpi-icono verde"><ClipboardList/></span><div><small>Ventas realizadas</small><strong data-testid="cantidad-ventas">{ventas.cantidadVentas}</strong><span className="reportes-kpi-nota">Cobros completados</span></div></article>
          <article className="reportes-kpi tarjeta"><span className="reportes-kpi-icono ambar"><Wallet/></span><div><small>Ticket promedio</small><strong>{soles(ventas.ticketPromedio ?? 0)}</strong><span className="reportes-kpi-nota">Por venta</span></div></article>
          <article className="reportes-kpi tarjeta"><span className="reportes-kpi-icono azul"><ShoppingBag/></span><div><small>Pedidos registrados</small><strong>{pedidos?.total ?? '—'}</strong><span className="reportes-kpi-nota">En el periodo seleccionado</span></div></article>
        </section>

        <section className="reportes-graficos">
          <article className="tarjeta reportes-panel reportes-evolucion"><div className="reportes-panel-titulo"><div><h3><ChartNoAxesCombined size={19}/> Ingresos por periodo</h3><small>Ventas cobradas a lo largo del tiempo</small></div>
            <div className="reportes-agrupacion no-imprimir">{['dia','semana','mes'].map((g)=><button key={g} className={agrupacion===g?'activo':''} onClick={()=>setAgrupacion(g)}>{g==='dia'?'Día':g==='semana'?'Semana':'Mes'}</button>)}</div></div>
          {serieChart.length ? <div className="reportes-chart-area"><ResponsiveContainer width="100%" height={270}><AreaChart data={serieChart} margin={{top:15,right:15,left:2,bottom:5}}><defs><linearGradient id="reporteIngreso" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stopColor="#e87508" stopOpacity={0.24}/><stop offset="95%" stopColor="#e87508" stopOpacity={0.02}/></linearGradient></defs><CartesianGrid strokeDasharray="4 4" stroke="#f0dfca"/><XAxis dataKey="fecha" tick={{fontSize:12,fill:'#795a48'}} axisLine={{stroke:'#d8c1a3'}}/><YAxis tick={{fontSize:12,fill:'#795a48'}} tickFormatter={(v)=>`S/${v}`} axisLine={false}/><Tooltip formatter={(v)=>soles(v)} labelStyle={{color:'#54280d'}}/><Area type="monotone" dataKey="monto" name="Ingresos" stroke="#e87508" strokeWidth={3} fill="url(#reporteIngreso)" activeDot={{r:5}}/></AreaChart></ResponsiveContainer></div> : <p className="nota reportes-vacio">No hay ventas registradas para este rango.</p>}
          <div className="reportes-mini-resumen"><span><b>{serie.reduce((s,p)=>s+Number(p.ventas||0),0)}</b> ventas en el periodo</span><span><b>{soles(serie.reduce((s,p)=>s+Number(p.monto||0),0))}</b> acumulados</span></div>
        </article>
        <article className="tarjeta reportes-panel reportes-categorias"><div className="reportes-panel-titulo"><div><h3>Ventas por categoría</h3><small>Distribución de ingresos</small></div></div>
          {categoriasChart.length ? <div className="reportes-donut-contenido"><div className="reportes-donut"><ResponsiveContainer width="100%" height={220}><PieChart><Pie data={categoriasChart} dataKey="value" nameKey="name" innerRadius={62} outerRadius={88} paddingAngle={3}>{categoriasChart.map((_,i)=><Cell key={i} fill={COLORES[i%COLORES.length]}/>)}</Pie><Tooltip formatter={(v)=>soles(v)}/></PieChart></ResponsiveContainer><div className="reportes-donut-centro"><strong>{soles(ventas.totalVendido)}</strong><small>Total</small></div></div><div className="reportes-leyenda">{categorias.map((c,i)=><div key={c.nombre}><span className="reportes-punto" style={{background:COLORES[i%COLORES.length]}}/><span>{c.nombre}</span><b>{porcentaje(c.monto,ventas.totalVendido)}</b><strong>{soles(c.monto)}</strong></div>)}</div></div> : <p className="nota reportes-vacio">Sin ventas por categoría.</p>}
        </article>
      </section>

      <section className="reportes-inferior">
        <article className="tarjeta reportes-panel reportes-ranking"><div className="reportes-panel-titulo"><div><h3><Trophy size={18}/> Productos más vendidos</h3><small>Top de unidades vendidas</small></div></div>
          {masVendidos.length ? <div className="reportes-ranking-lista">{masVendidos.map((p,i)=>(
            <div className="reportes-ranking-item" key={p.nombre}>
              <span className="reportes-ranking-num">{i+1}</span>
              <div className="reportes-ranking-info">
                <div><strong>{p.nombre}</strong><span>{p.cantidad} unidades</span></div>
                <div className="reportes-ranking-barra"><span style={{width:`${Number(p.cantidad)/maxProducto*100}%`}} /></div>
              </div>
            </div>
          ))}</div> : <p className="nota reportes-vacio">No hay productos vendidos.</p>}
        </article>
        <article className="tarjeta reportes-panel reportes-metodos"><div className="reportes-panel-titulo"><div><h3>Métodos de pago</h3><small>Ingresos según forma de cobro</small></div></div>
          {metodos.length ? <div className="reportes-metodos-lista">{metodos.map((m,i)=><div key={m.nombre}><span className="reportes-punto" style={{background:COLORES[i%COLORES.length]}}/><div><strong>{m.nombre}</strong><small>{porcentaje(m.monto,ventas.totalVendido)} del total</small></div><b>{soles(m.monto)}</b></div>)}</div> : <p className="nota reportes-vacio">Sin métodos de pago registrados.</p>}
        </article>
        <article className="tarjeta reportes-panel reportes-cajeros"><div className="reportes-panel-titulo"><div><h3>Ventas por cajero</h3><small>Ingresos registrados por usuario</small></div></div>
          {cajeros.length ? <div className="reportes-cajeros-lista">{cajeros.map((c)=><div key={c.nombre}><span className="reportes-avatar">{c.nombre?.trim()?.charAt(0)?.toUpperCase()||'C'}</span><span>{c.nombre}</span><strong>{soles(c.monto)}</strong></div>)}</div> : <p className="nota reportes-vacio">Sin ventas por cajero.</p>}
        </article>
      </section>

      <section className="reportes-inventario tarjeta reportes-panel"><div className="reportes-panel-titulo"><div><h3><Package size={19}/> Movimientos de inventario</h3><small>Entradas y salidas en las fechas seleccionadas</small></div><div className="reportes-inventario-totales"><span>Entradas <b>{totalEntradas.toFixed(2)}</b></span><span>Salidas <b>{totalSalidas.toFixed(2)}</b></span></div></div>
        {inventario?.insumos?.length ? <div className="reportes-tabla-scroll"><table className="reportes-tabla"><thead><tr><th>Insumo</th><th>Unidad</th><th>Entradas</th><th>Salidas</th><th>Stock actual</th></tr></thead><tbody>{inventario.insumos.map((i)=><tr key={i.insumo}><td><strong>{i.insumo}</strong></td><td>{i.unidad}</td><td className="reportes-cantidad-entrada">+{Number(i.entradas||0).toFixed(2)}</td><td className="reportes-cantidad-salida">−{Number(i.salidas||0).toFixed(2)}</td><td><strong>{Number(i.stockActual||0).toFixed(2)}</strong> <small>{i.unidad}</small></td></tr>)}</tbody></table></div> : <p className="nota reportes-vacio">No hay insumos registrados.</p>}
      </section>
      {pedidos?.porEstado?.length > 0 && <section className="reportes-pedidos tarjeta reportes-panel"><div className="reportes-panel-titulo"><div><h3>Pedidos por estado</h3><small>Pedidos generados dentro del periodo</small></div><span className="reportes-badge">{pedidos.total} pedidos</span></div><div className="reportes-estados">{pedidos.porEstado.map((e,i)=><div key={e.nombre}><span className="reportes-estado-color" style={{background:COLORES[i%COLORES.length]}}/><span>{e.nombre}</span><strong>{e.cantidad}</strong></div>)}</div></section>}
      </>}
    </main>
  );
}
