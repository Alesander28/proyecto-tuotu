import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Area, AreaChart, Bar, BarChart, CartesianGrid, Cell, Legend, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { Armchair, Banknote, ClipboardList, PackageX, Receipt, Timer } from 'lucide-react';
import { api } from '../../comun/api.js';
import { fechaHora, soles } from '../../comun/formato.js';
import { consulta, periodoAnterior } from '../../comun/periodos.js';
import { Esqueleto, FiltroPeriodo, InsigniaPedido, InsigniaStock, KpiCard, Tendencia, Vacio, periodoInicial } from '../../comun/ui.jsx';
import './gerente.css';

const COLORES = ['#e87508', '#71370f', '#f59a24', '#b8570a', '#a66b3f', '#d9a25f', '#4a210c', '#c98a4b'];
export const ETIQUETA_TIPO = {
  PEDIDO_REGISTRADO: 'Registró un pedido', CAMBIO_ESTADO_PEDIDO: 'Cambió el estado de un pedido', VENTA_REGISTRADA: 'Registró una venta',
  ENTRADA_INSUMO: 'Registró una entrada de inventario', SALIDA_INSUMO: 'Registró una salida de inventario', INSUMO_CREADO: 'Creó un insumo',
  STOCK_MINIMO: 'Cambió un stock mínimo', PRODUCTO_CREADO: 'Creó un producto', PRODUCTO_ACTUALIZADO: 'Modificó un producto',
  PRODUCTO_ACTIVADO: 'Activó un producto', PRODUCTO_DESACTIVADO: 'Desactivó un producto', PRECIO_ACTUALIZADO: 'Cambió un precio',
  CAJA_ABIERTA: 'Abrió una caja', CAJA_CERRADA: 'Cerró una caja', USUARIO_CREADO: 'Creó un usuario', USUARIO_ACTUALIZADO: 'Modificó un usuario',
  USUARIO_DESACTIVADO: 'Desactivó un usuario', INICIO_SESION: 'Inició sesión', CIERRE_SESION: 'Cerró sesión', MESA_CREADA: 'Registró una mesa',
};

export function etiquetaPeriodo(iso, agrupar) {
  const f = new Date(iso + 'T00:00:00');
  if (agrupar === 'mes') return f.toLocaleDateString('es-PE', { month: 'short', year: '2-digit' });
  const base = f.toLocaleDateString('es-PE', { day: '2-digit', month: '2-digit' });
  return agrupar === 'semana' ? 'Sem. ' + base : base;
}

const dinero = (v) => soles(v);

// Panel del gerente: todas las cifras salen del servidor.
export default function Dashboard() {
  const [resumen, setResumen] = useState(null);
  const [recientes, setRecientes] = useState(null);
  const [actividad, setActividad] = useState(null);
  const [periodo, setPeriodo] = useState(periodoInicial('mes'));
  const [agrupar, setAgrupar] = useState('dia');
  const [serie, setSerie] = useState(null);
  const [ventas, setVentas] = useState(null);
  const [anterior, setAnterior] = useState(null);
  const [error, setError] = useState('');

  const cargarFijos = useCallback(async () => {
    try {
      const [r, p, a] = await Promise.all([api('/reportes/resumen'), api('/pedidos/buscar'), api('/auditoria/recientes')]);
      setResumen(r);
      setRecientes(p.slice(0, 8));
      setActividad(a);
      setError('');
    } catch (e) {
      setError(e.message);
    }
  }, []);

  const cargarPeriodo = useCallback(async () => {
    const { desde, hasta } = periodo;
    if (!desde || !hasta || hasta < desde) return;
    const previo = periodoAnterior({ desde, hasta });
    setSerie(null);
    setVentas(null);
    setAnterior(null);
    try {
      const [s, v, a] = await Promise.all([
        api('/reportes/serie' + consulta({ desde, hasta, agrupar })),
        api('/reportes/ventas' + consulta({ desde, hasta })),
        api('/reportes/ventas' + consulta(previo)),
      ]);
      setSerie(s);
      setVentas(v);
      setAnterior(a);
    } catch (e) {
      setError(e.message);
    }
  }, [periodo, agrupar]);

  useEffect(() => {
    cargarFijos();
    const reloj = setInterval(cargarFijos, 60000);
    return () => clearInterval(reloj);
  }, [cargarFijos]);
  useEffect(() => {
    cargarPeriodo();
  }, [cargarPeriodo]);

  const r = resumen;
  const datosSerie = (serie ?? []).map((p) => ({ nombre: etiquetaPeriodo(p.periodo, agrupar), Ventas: Number(p.monto), Operaciones: p.ventas }));
  const datosCategoria = (ventas?.porCategoria ?? []).map((c) => ({ nombre: c.nombre, value: Number(c.monto) }));
  const datosMasVendidos = (ventas?.masVendidos ?? []).map((p) => ({ nombre: p.nombre, Unidades: p.cantidad }));
  const datosComparacion = ventas && anterior ? [
    { nombre: 'Periodo anterior', Ingresos: Number(anterior.totalVendido) },
    { nombre: 'Periodo seleccionado', Ingresos: Number(ventas.totalVendido) },
  ] : [];

  return (
    <div className="dashboard-gerente">
      <section className="gerente-banner" aria-label="Mensaje del restaurante">
        <div className="gerente-banner-contenido">
          <h2><span className="gerente-frase-clara">Grandes sabores,</span><br /><span>mejores momentos</span></h2>
          <div className="gerente-banner-linea" />
        </div>
      </section>
      {error && <p className="error" role="alert">{error}</p>}

      <section className="cuadricula-kpi gerente-kpis" aria-label="Indicadores principales de hoy">
        <KpiCard icono={Receipt} titulo="Ventas de hoy" valor={r && dinero(r.ventasHoyMonto)} tono="verde" cargando={!r}
                 nota={r && `${r.ventasHoyCantidad} venta(s) · ticket promedio ${dinero(r.ticketPromedio)}`}>
          {r && <Tendencia actual={r.ventasHoyMonto} anterior={r.ventasAyerMonto} etiqueta="vs. ayer" />}
        </KpiCard>
        <KpiCard icono={ClipboardList} titulo="Pedidos de hoy" valor={r?.pedidosHoy} cargando={!r}>
          {r && <Tendencia actual={r.pedidosHoy} anterior={r.pedidosAyer} etiqueta="vs. ayer" />}
        </KpiCard>
        <KpiCard icono={PackageX} titulo="Insumos con stock bajo" valor={r?.insumosStockBajo} tono={r?.insumosStockBajo > 0 ? 'rojo' : 'verde'} cargando={!r}
                 nota={r && `${r.insumosAgotados} agotado(s)`} />
        <KpiCard icono={Timer} titulo="Pedidos pendientes" valor={r?.pedidosPendientes} tono="info" cargando={!r}
                 nota={r && `${r.pedidosEnPreparacion} en preparación`} />
      </section>
      <section className="gerente-estados-rapidos" aria-label="Estados adicionales">
        <div className="gerente-estado"><Armchair size={18} /><span>Mesas atendidas hoy</span><strong>{r ? r.mesasAtendidasHoy : '—'}</strong></div>
        <div className="gerente-estado"><Banknote size={18} /><span>Cajas abiertas</span><strong>{r ? r.cajasAbiertas : '—'}</strong></div>
      </section>

      <FiltroPeriodo valor={periodo} onChange={setPeriodo} />

      <div className="rejilla-3-2 gerente-graficos">
        <section className="tarjeta" aria-label="Ventas en el tiempo">
          <div className="tarjeta-cabecera">
            <h3>Ventas por {agrupar === 'dia' ? 'día' : agrupar === 'semana' ? 'semana' : 'mes'}</h3>
            <div className="conmutador" role="group" aria-label="Agrupar por">
              {[['dia', 'Día'], ['semana', 'Semana'], ['mes', 'Mes']].map(([v, t]) => (
                <button key={v} type="button" aria-pressed={agrupar === v} onClick={() => setAgrupar(v)}>{t}</button>
              ))}
            </div>
          </div>
          {!serie ? <Esqueleto alto={290} /> : (
            <div className="grafico">
              <ResponsiveContainer width="100%" height="100%">
                <AreaChart data={datosSerie} margin={{ top: 8, right: 12, left: 0, bottom: 0 }}>
                  <defs>
                    <linearGradient id="degradado" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#e87508" stopOpacity={0.55} />
                      <stop offset="95%" stopColor="#e87508" stopOpacity={0.05} />
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" stroke="#ead7bc" />
                  <XAxis dataKey="nombre" tick={{ fontSize: 12, fill: '#76543b' }} />
                  <YAxis tick={{ fontSize: 12, fill: '#76543b' }} tickFormatter={(v) => `S/ ${v}`} width={64} />
                  <Tooltip formatter={(v, n) => (n === 'Ventas' ? dinero(v) : v)} />
                  <Legend />
                  <Area type="monotone" dataKey="Ventas" stroke="#71370f" strokeWidth={2} fill="url(#degradado)" />
                </AreaChart>
              </ResponsiveContainer>
            </div>
          )}
        </section>

        <section className="tarjeta" aria-label="Comparación de ingresos">
          <div className="tarjeta-cabecera"><h3>Comparación de ingresos</h3></div>
          {!ventas || !anterior ? <Esqueleto alto={220} /> : (
            <>
              <div className="grafico" style={{ height: 220 }}>
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={datosComparacion} margin={{ top: 8, right: 12, left: 0, bottom: 0 }}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#ead7bc" />
                    <XAxis dataKey="nombre" tick={{ fontSize: 12, fill: '#76543b' }} />
                    <YAxis tick={{ fontSize: 12, fill: '#76543b' }} tickFormatter={(v) => `S/ ${v}`} width={64} />
                    <Tooltip formatter={(v) => dinero(v)} />
                    <Bar dataKey="Ingresos" radius={[8, 8, 0, 0]}>
                      <Cell fill="#d9a25f" />
                      <Cell fill="#e87508" />
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              </div>
              <p><strong>{dinero(ventas.totalVendido)}</strong> frente a {dinero(anterior.totalVendido)} <Tendencia actual={ventas.totalVendido} anterior={anterior.totalVendido} /></p>
              <p className="nota">Son ingresos (lo cobrado), no ganancias: el sistema no registra costos.</p>
            </>
          )}
        </section>
      </div>

      <div className="rejilla-2 gerente-graficos">
        <section className="tarjeta" aria-label="Ventas por categoría">
          <div className="tarjeta-cabecera"><h3>Ventas por categoría</h3></div>
          {!ventas ? <Esqueleto alto={290} /> : datosCategoria.length === 0 ? <Vacio texto="Sin ventas en este periodo." /> : (
            <div className="grafico">
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie data={datosCategoria} dataKey="value" nameKey="nombre" innerRadius={55} outerRadius={100} paddingAngle={2}>
                    {datosCategoria.map((c, i) => <Cell key={c.nombre} fill={COLORES[i % COLORES.length]} />)}
                  </Pie>
                  <Tooltip formatter={(v) => dinero(v)} />
                  <Legend />
                </PieChart>
              </ResponsiveContainer>
            </div>
          )}
        </section>
        <section className="tarjeta" aria-label="Productos más vendidos">
          <div className="tarjeta-cabecera"><h3>Productos más vendidos</h3></div>
          {!ventas ? <Esqueleto alto={290} /> : datosMasVendidos.length === 0 ? <Vacio texto="Sin ventas en este periodo." /> : (
            <div className="grafico">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={datosMasVendidos} layout="vertical" margin={{ top: 4, right: 16, left: 10, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#ead7bc" />
                  <XAxis type="number" tick={{ fontSize: 12, fill: '#76543b' }} allowDecimals={false} />
                  <YAxis type="category" dataKey="nombre" width={140} tick={{ fontSize: 12, fill: '#76543b' }} />
                  <Tooltip />
                  <Legend />
                  <Bar dataKey="Unidades" fill="#e87508" radius={[0, 8, 8, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          )}
        </section>
      </div>

      <section className="tarjeta gerente-pedidos" aria-label="Pedidos recientes" style={{ marginBottom: 14 }}>
        <div className="tarjeta-cabecera"><h3>Pedidos recientes</h3><Link className="enlace" to="/pedidos">Ver todos</Link></div>
        {!recientes ? <Esqueleto alto={160} /> : recientes.length === 0 ? <Vacio icono={ClipboardList} texto="Todavía no hay pedidos." /> : (
          <div className="contenedor-tabla">
            <table className="tabla">
              <thead><tr><th>N.°</th><th>Fecha y hora</th><th>Mesa</th><th>Mozo</th><th className="num">Total</th><th>Estado</th><th><span className="solo-lectores">Acción</span></th></tr></thead>
              <tbody>
                {recientes.map((p) => (
                  <tr key={p.id}>
                    <td><strong>{p.codigo}</strong></td>
                    <td>{fechaHora(p.fechaHora)}</td>
                    <td>{p.mesa}</td>
                    <td>{p.mozo}</td>
                    <td className="num">{dinero(p.total)}</td>
                    <td><InsigniaPedido estado={p.estado} /></td>
                    <td><Link className="enlace" to={`/pedidos?codigo=${p.codigo}`}>Ver detalle</Link></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <div className="rejilla-2 gerente-graficos gerente-final">
        <section className="tarjeta" aria-label="Insumos con stock bajo">
          <div className="tarjeta-cabecera"><h3>Insumos con stock bajo</h3><Link className="enlace" to="/inventario">Ir a inventario</Link></div>
          {!r ? <Esqueleto alto={160} /> : r.alertasStock.length === 0 ? <Vacio icono={PackageX} texto="No hay insumos con stock bajo. Defina el stock mínimo de cada insumo en Inventario para recibir avisos." /> : (
            <div className="contenedor-tabla">
              <table className="tabla">
                <thead><tr><th>Insumo</th><th className="num">Stock actual</th><th className="num">Mínimo</th><th>Estado</th><th><span className="solo-lectores">Acción</span></th></tr></thead>
                <tbody>
                  {r.alertasStock.map((a) => (
                    <tr key={a.insumo}>
                      <td>{a.insumo}</td>
                      <td className="num">{Number(a.stockActual).toFixed(2)} {a.unidad}</td>
                      <td className="num">{Number(a.stockMinimo).toFixed(2)}</td>
                      <td><InsigniaStock estado={a.estado} /></td>
                      <td><Link className="enlace" to={`/inventario?q=${encodeURIComponent(a.insumo)}`}>Consultar</Link></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
        <section className="tarjeta" aria-label="Actividad reciente">
          <div className="tarjeta-cabecera"><h3>Actividad reciente</h3><Link className="enlace" to="/auditoria">Ver auditoría</Link></div>
          {!actividad ? <Esqueleto alto={160} /> : actividad.length === 0 ? <Vacio texto="Todavía no hay actividad registrada." /> : (
            <ul className="lista-actividad">
              {actividad.map((a) => (
                <li key={a.id}>
                  <div><strong>{a.usuario}</strong> · {ETIQUETA_TIPO[a.tipo] ?? a.tipo}</div>
                  <div className="nota">{a.descripcion}</div>
                  <small>{fechaHora(a.fechaHora)} · {a.modulo}</small>
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>
    </div>
  );
}
