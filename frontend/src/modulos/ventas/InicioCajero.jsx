import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Banknote, ClipboardList, Clock3, ReceiptText, Wallet, ArrowRight, RefreshCw } from 'lucide-react';
import { api } from '../../comun/api.js';
import { soles, fechaLarga } from '../../comun/formato.js';
import './ventas.css';

const hoyLocal = () => {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
};

export default function InicioCajero() {
  const [caja, setCaja] = useState(null);
  const [ventas, setVentas] = useState([]);
  const [pendientes, setPendientes] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState('');

  const cargar = useCallback(async () => {
    setCargando(true);
    try {
      const [actual, listaVentas, listaPendientes] = await Promise.all([
        api('/cajas/actual'), api('/ventas'), api('/ventas/pendientes'),
      ]);
      setCaja(actual);
      setVentas(listaVentas || []);
      setPendientes(listaPendientes || []);
      setError('');
    } catch (e) {
      setError(e.message || 'No se pudo cargar el resumen de caja.');
    } finally {
      setCargando(false);
    }
  }, []);

  useEffect(() => {
    cargar();
    const reloj = setInterval(cargar, 30000);
    return () => clearInterval(reloj);
  }, [cargar]);

  const ventasHoy = ventas.filter((v) => v.fechaHora && new Date(v.fechaHora).toLocaleDateString('en-CA') === hoyLocal());
  const totalHoy = ventasHoy.reduce((suma, v) => suma + Number(v.total || 0), 0);
  const recientes = ventas.slice(0, 5);

  return (
    <section className="modulo-cajero inicio-cajero">
      <div className="fila-titulo">
        <div>
          <h2>¡Hola, Cajero!</h2>
          <p>Controla los cobros y movimientos de caja de TÚ O TÚ.</p>
          <small className="inicio-cajero-fecha">{fechaLarga()}</small>
        </div>
        <button className="boton secundario" onClick={cargar} disabled={cargando}>
          <RefreshCw size={16} /> Actualizar
        </button>
      </div>

      {error && <p className="error" role="alert">{error}</p>}

      <div className="inicio-cajero-banner">
        <div>
          <span className={`insignia ${caja ? 'verde' : 'neutro'}`}>{caja ? 'Caja abierta' : 'Caja cerrada'}</span>
          <h3>{caja ? 'Tu turno está en curso' : 'Comienza tu jornada'}</h3>
          <p>{caja ? 'Ya puedes cobrar los pedidos atendidos y revisar tus movimientos.' : 'Abre tu caja registrando el monto inicial para habilitar los cobros.'}</p>
        </div>
        <Link className="boton" to="/caja">{caja ? 'Ir a caja' : 'Abrir caja'} <ArrowRight size={17} /></Link>
      </div>

      <div className="inicio-cajero-kpis">
        <article className="tarjeta inicio-cajero-kpi">
          <span className="inicio-cajero-icono"><ClipboardList /></span>
          <div><small>Pedidos por cobrar</small><strong>{cargando ? '—' : pendientes.length.toString().padStart(2, '0')}</strong><span>Pedidos atendidos</span></div>
        </article>
        <article className="tarjeta inicio-cajero-kpi">
          <span className="inicio-cajero-icono"><Wallet /></span>
          <div><small>Estado de caja</small><strong className={caja ? 'texto-verde' : 'texto-naranja'}>{caja ? 'Abierta' : 'Cerrada'}</strong><span>{caja ? `Inicio: ${new Date(caja.fechaApertura).toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' })}` : 'No hay turno activo'}</span></div>
        </article>
        <article className="tarjeta inicio-cajero-kpi">
          <span className="inicio-cajero-icono"><Banknote /></span>
          <div><small>Ventas de hoy</small><strong>{cargando ? '—' : soles(totalHoy)}</strong><span>{ventasHoy.length} cobro(s) registrado(s)</span></div>
        </article>
        <article className="tarjeta inicio-cajero-kpi">
          <span className="inicio-cajero-icono"><ReceiptText /></span>
          <div><small>Ventas en tu turno</small><strong>{cargando ? '—' : soles(caja?.totalVentas || 0)}</strong><span>{caja?.cantidadVentas || 0} cobro(s)</span></div>
        </article>
      </div>

      <div className="inicio-cajero-columnas">
        <section className="tarjeta inicio-cajero-atajos">
          <div className="tarjeta-cabecera"><h3>Accesos rápidos</h3></div>
          <Link to="/pedidos" className="inicio-cajero-acceso"><span className="inicio-cajero-icono"><ClipboardList /></span><span><strong>Consultar pedidos</strong><small>Revisa el estado de los pedidos del restaurante</small></span><ArrowRight /></Link>
          <Link to="/caja" className="inicio-cajero-acceso"><span className="inicio-cajero-icono"><Wallet /></span><span><strong>Gestionar caja</strong><small>Abrir turno, registrar cobros y cerrar caja</small></span><ArrowRight /></Link>
        </section>

        <section className="tarjeta inicio-cajero-historial">
          <div className="tarjeta-cabecera"><h3><Clock3 size={18} /> Últimos cobros</h3><Link to="/caja" className="enlace">Ver todos <ArrowRight size={15} /></Link></div>
          <div className="contenedor-tabla">
            <table className="tabla">
              <thead><tr><th>Pedido</th><th>Mesa</th><th>Método</th><th>Total</th></tr></thead>
              <tbody>
                {recientes.map((v) => <tr key={v.id}><td>{v.pedidoCodigo}</td><td>{v.mesa || '—'}</td><td>{v.metodoPago}</td><td>{soles(v.total)}</td></tr>)}
                {!cargando && recientes.length === 0 && <tr><td colSpan={4} className="nota">Todavía no hay cobros registrados.</td></tr>}
                {cargando && <tr><td colSpan={4} className="nota">Cargando movimientos…</td></tr>}
              </tbody>
            </table>
          </div>
        </section>
      </div>
    </section>
  );
}
