import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { Armchair, ArrowRight, ClipboardList, Clock3, Plus, RefreshCw, Utensils } from 'lucide-react';
import { api } from '../../comun/api.js';
import { useAuth } from '../seguridad/auth.jsx';
import { soles } from '../../comun/formato.js';
import './mozo.css';
import './mozo-redisenio.css';

const ESTADOS = [
  { id: 'PENDIENTE', nombre: 'Pendientes', clase: 'pendiente' },
  { id: 'EN_PREPARACION', nombre: 'En preparación', clase: 'preparacion' },
  { id: 'ATENDIDO', nombre: 'Atendidos', clase: 'atendido' },
];

function fechaHoy() {
  const ahora = new Date();
  return `${ahora.getFullYear()}-${String(ahora.getMonth() + 1).padStart(2, '0')}-${String(ahora.getDate()).padStart(2, '0')}`;
}

function hora(fecha) {
  return new Date(fecha).toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' });
}

// Panel de trabajo del mozo: usa los pedidos reales registrados por el usuario autenticado.
export default function InicioMozo() {
  const { usuario } = useAuth();
  const [pedidos, setPedidos] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState('');

  const cargar = useCallback(async () => {
    try {
      const datos = await api('/pedidos');
      setPedidos(Array.isArray(datos) ? datos : []);
      setError('');
    } catch (e) {
      setError(e.message || 'No se pudieron cargar los pedidos.');
    } finally {
      setCargando(false);
    }
  }, []);

  useEffect(() => {
    cargar();
    const reloj = setInterval(cargar, 15000);
    return () => clearInterval(reloj);
  }, [cargar]);

  const misPedidosHoy = useMemo(() => {
    const hoy = fechaHoy();
    return pedidos.filter((p) => p.mozo === usuario.nombreCompleto && String(p.fechaHora).slice(0, 10) === hoy);
  }, [pedidos, usuario.nombreCompleto]);

  const recientes = useMemo(() => [...misPedidosHoy].sort((a, b) => new Date(b.fechaHora) - new Date(a.fechaHora)).slice(0, 6), [misPedidosHoy]);
  const totalHoy = misPedidosHoy.reduce((suma, p) => suma + Number(p.total || 0), 0);

  return (
    <div className="mozo-inicio">
      <div className="mozo-bienvenida">
        <div>
          <span className="mozo-eyebrow">ÁREA DE ATENCIÓN</span>
          <h2>¡Hola, {usuario.nombreCompleto}!</h2>
          <p>Gestiona tus pedidos y brinda una excelente atención a tus clientes.</p>
        </div>
        <div className="mozo-saludo-icono" aria-hidden="true"><Utensils size={34} /></div>
      </div>

      <div className="mozo-acciones-principales">
        <Link to="/pedidos/nuevo" className="mozo-accion crear">
          <span className="mozo-accion-icono"><Plus size={27} /></span>
          <span><strong>Nuevo pedido</strong><small>Registrar el pedido de una mesa</small></span>
          <ArrowRight className="mozo-flecha" size={20} />
        </Link>
        <Link to="/mesas" className="mozo-accion seguimiento">
          <span className="mozo-accion-icono"><Armchair size={27} /></span>
          <span><strong>Gestión de mesas</strong><small>Consultar mesas libres y ocupadas</small></span>
          <ArrowRight className="mozo-flecha" size={20} />
        </Link>
        <Link to="/pedidos" className="mozo-accion seguimiento">
          <span className="mozo-accion-icono"><ClipboardList size={27} /></span>
          <span><strong>Mis pedidos</strong><small>Consultar y actualizar estados</small></span>
          <ArrowRight className="mozo-flecha" size={20} />
        </Link>
      </div>

      <div className="mozo-seccion-cabecera">
        <div><h3>Resumen de hoy</h3><p>Actividad de tus pedidos registrados durante el día.</p></div>
        <button type="button" className="boton secundario" onClick={cargar} disabled={cargando}>
          <RefreshCw size={16} /> Actualizar
        </button>
      </div>

      {error && <p className="error" role="alert">{error}</p>}

      <div className="mozo-indicadores">
        <article className="mozo-indicador">
          <span className="mozo-indicador-icono naranja"><ClipboardList size={21} /></span>
          <div><small>Pedidos registrados</small><strong>{cargando ? '—' : misPedidosHoy.length}</strong></div>
        </article>
        {ESTADOS.map((estado) => (
          <article className="mozo-indicador" key={estado.id}>
            <span className={`mozo-indicador-icono ${estado.clase}`}><Clock3 size={21} /></span>
            <div><small>{estado.nombre}</small><strong>{cargando ? '—' : misPedidosHoy.filter((p) => p.estado === estado.id).length}</strong></div>
          </article>
        ))}
        <article className="mozo-indicador">
          <span className="mozo-indicador-icono cafe"><Armchair size={21} /></span>
          <div><small>Monto de tus pedidos</small><strong>{cargando ? '—' : soles(totalHoy)}</strong></div>
        </article>
      </div>

      <section className="mozo-recientes tarjeta">
        <div className="mozo-seccion-cabecera">
          <div><h3>Pedidos recientes</h3><p>Los últimos pedidos que registraste hoy.</p></div>
          <Link to="/pedidos" className="enlace">Ver todos <ArrowRight size={15} /></Link>
        </div>
        {cargando ? <p className="nota">Cargando pedidos…</p> : recientes.length === 0 ? (
          <div className="mozo-vacio">
            <span><ClipboardList size={28} /></span>
            <strong>Aún no tienes pedidos registrados hoy</strong>
            <p>Cuando registres un pedido, aparecerá aquí.</p>
            <Link to="/pedidos/nuevo" className="boton">Crear primer pedido</Link>
          </div>
        ) : (
          <div className="mozo-tabla-contenedor">
            <table className="mozo-tabla">
              <thead><tr><th>Pedido</th><th>Mesa</th><th>Hora</th><th>Productos</th><th>Total</th><th>Estado</th></tr></thead>
              <tbody>{recientes.map((p) => (
                <tr key={p.id}>
                  <td><strong>{p.codigo}</strong></td>
                  <td>{p.mesa}</td>
                  <td>{hora(p.fechaHora)}</td>
                  <td>{p.detalles?.reduce((n, d) => n + Number(d.cantidad || 0), 0) || 0} unidades</td>
                  <td><strong>{soles(p.total)}</strong></td>
                  <td><span className={`mozo-estado ${String(p.estado).toLowerCase()}`}>{ESTADOS.find((e) => e.id === p.estado)?.nombre || p.estado}</span></td>
                </tr>
              ))}</tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}
