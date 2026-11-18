import { useCallback, useEffect, useState } from 'react';
import { abrirPdf, api } from '../../comun/api.js';
import { soles } from '../../comun/formato.js';
import { Banknote, Clock3, ReceiptText, RefreshCw, Wallet } from 'lucide-react';
import './ventas.css';

// Operación del cajero: apertura de turno, cobro de pedidos y cierre/arqueo de caja.
export default function Caja() {
  const [caja, setCaja] = useState(null);
  const [pendientes, setPendientes] = useState([]);
  const [metodos, setMetodos] = useState([]);
  const [recientes, setRecientes] = useState([]);
  const [elegido, setElegido] = useState(null);
  const [metodo, setMetodo] = useState('');
  const [montoInicial, setMontoInicial] = useState('');
  const [montoContado, setMontoContado] = useState('');
  const [observacion, setObservacion] = useState('');
  const [ultima, setUltima] = useState(null);
  const [ultimoCierre, setUltimoCierre] = useState(null);
  const [error, setError] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [cargando, setCargando] = useState(true);

  const cargar = useCallback(async (limpiarError = true) => {
    try {
      const [actual, p, m, r] = await Promise.all([
        api('/cajas/actual'), api('/ventas/pendientes'), api('/ventas/metodos-pago'), api('/ventas'),
      ]);
      setCaja(actual);
      setPendientes(p || []);
      setMetodos(m || []);
      setRecientes((r || []).slice(0, 10));
      setMetodo((actualMetodo) => actualMetodo || m?.[0] || '');
      if (limpiarError) setError('');
    } catch (e) {
      setError(e.message || 'No se pudo cargar la información de caja.');
    } finally {
      setCargando(false);
    }
  }, []);

  useEffect(() => {
    cargar();
    const reloj = setInterval(() => cargar(false), 15000);
    return () => clearInterval(reloj);
  }, [cargar]);

  async function abrir(e) {
    e.preventDefault();
    setError('');
    setEnviando(true);
    try {
      await api('/cajas/abrir', { metodo: 'POST', cuerpo: { montoInicial: Number(montoInicial) } });
      setMontoInicial('');
      setUltimoCierre(null);
      await cargar();
    } catch (e) {
      setError(e.message);
    } finally {
      setEnviando(false);
    }
  }

  async function cerrar(e) {
    e.preventDefault();
    setError('');
    setEnviando(true);
    try {
      const resultado = await api('/cajas/cerrar', {
        metodo: 'POST', cuerpo: { montoContado: Number(montoContado), observacion: observacion.trim() || null },
      });
      setUltimoCierre(resultado);
      setCaja(null);
      setElegido(null);
      setMontoContado('');
      setObservacion('');
      await cargar();
    } catch (e) {
      setError(e.message);
    } finally {
      setEnviando(false);
    }
  }

  async function registrar() {
    if (!caja || !elegido || !metodo) return;
    setError('');
    setEnviando(true);
    try {
      const venta = await api('/ventas', { metodo: 'POST', cuerpo: { pedidoId: elegido.id, metodoPago: metodo } });
      setUltima(venta);
      setElegido(null);
      await cargar();
    } catch (e) {
      setError(e.message);
      await cargar(false);
    } finally {
      setEnviando(false);
    }
  }

  async function verBoleta(comprobanteId) {
    try {
      await abrirPdf(`/comprobantes/${comprobanteId}/pdf`);
    } catch (e) {
      setError(e.message);
    }
  }

  const hora = (f) => f ? new Date(f).toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' }) : '—';
  const fechaHora = (f) => f ? new Date(f).toLocaleString('es-PE', { dateStyle: 'medium', timeStyle: 'short' }) : '—';

  return (
    <section className="modulo-cajero">
      <div className="fila-titulo cajero-header">
        <div className="cajero-header-texto">
          <span className="cajero-eyebrow">OPERACIÓN · TURNO Y COBROS</span>
          <h2>Panel de caja</h2>
          <p>Administra tu turno, cobra pedidos atendidos y emite las boletas de consumo.</p>
        </div>
        <button className="boton secundario" onClick={() => cargar()} disabled={cargando}>
          <RefreshCw size={16} /> Actualizar
        </button>
      </div>

      {error && <p className="error" role="alert">{error}</p>}
      {cargando && <p className="nota">Cargando información de caja…</p>}

      {caja ? (
        <>
          <div className="cajero-estado-turno cajero-banner-turno">
            <div>
              <span className="insignia verde">Caja abierta</span>
              <h3>Tu turno está en curso</h3>
              <p>Inicio: {fechaHora(caja.fechaApertura)}</p>
            </div>
            <div className="cajero-turno-id"><Wallet size={22} /><span>Turno #{caja.id}</span></div>
          </div>

          <div className="cajero-kpis cajero-kpis-premium">
            <article className="tarjeta cajero-kpi"><span className="cajero-kpi-icono"><Banknote /></span><div><small>Monto inicial</small><strong>{soles(caja.montoInicial)}</strong></div></article>
            <article className="tarjeta cajero-kpi"><span className="cajero-kpi-icono"><ReceiptText /></span><div><small>Ventas del turno</small><strong>{soles(caja.totalVentas)}</strong><small>{caja.cantidadVentas || 0} cobro(s)</small></div></article>
            <article className="tarjeta cajero-kpi"><span className="cajero-kpi-icono"><Wallet /></span><div><small>Efectivo esperado</small><strong>{soles(caja.efectivoEsperado)}</strong><small>Inicial + ventas en efectivo</small></div></article>
            <article className="tarjeta cajero-kpi"><span className="cajero-kpi-icono"><Clock3 /></span><div><small>Ventas en efectivo</small><strong>{soles(caja.ventasEfectivo)}</strong></div></article>
          </div>

          <div className="caja-pantalla cajero-layout-operacion">
            <section className="tarjeta cajero-pedidos cajero-panel-pedidos" aria-label="Pedidos atendidos">
              <div className="tarjeta-cabecera"><h3>Pedidos pendientes de cobro</h3><span className="insignia neutro">{pendientes.length} pendiente(s)</span></div>
              {pendientes.length === 0 && <p className="nota">No hay pedidos atendidos por cobrar.</p>}
              {pendientes.map((p) => (
                <button key={p.id} className={elegido?.id === p.id ? 'pedido-cobro elegido' : 'pedido-cobro'}
                  aria-label={`Seleccionar pedido ${p.codigo}`} onClick={() => setElegido(p)}>
                  <span><strong>Pedido {p.codigo}</strong><small>Mesa {p.mesa} · {p.mozo}</small></span>
                  <strong>{soles(p.total)}</strong>
                </button>
              ))}
            </section>

            <aside className="tarjeta resumen cajero-resumen cajero-panel-cobro" aria-label="Detalle de la venta">
              <h3>Detalle del cobro</h3>
              {!elegido && <p className="nota">Selecciona un pedido atendido para revisar su consumo.</p>}
              {elegido && (
                <>
                  <p className="cajero-dato-pedido">Pedido {elegido.codigo} · Mesa {elegido.mesa}</p>
                  <p className="nota">Atendió: {elegido.mozo}</p>
                  <div className="cajero-detalles">
                    {elegido.detalles.map((d, i) => (
                      <div key={i} className="linea-titulo">
                        <span>{d.cantidad} × {d.producto} ({d.presentacion}){d.extraQueso ? ' + extra queso' : ''}{d.paraLlevar ? ' · para llevar' : ''}</span>
                        <strong>{soles(d.subtotal)}</strong>
                      </div>
                    ))}
                  </div>
                  <div className="total"><span>Total a cobrar</span><strong data-testid="total-venta">{soles(elegido.total)}</strong></div>
                  <label htmlFor="metodo">Método de pago</label>
                  <select id="metodo" value={metodo} onChange={(e) => setMetodo(e.target.value)}>
                    {metodos.map((m) => <option key={m} value={m}>{m}</option>)}
                  </select>
                  <button className="boton" onClick={registrar} disabled={enviando || !metodo}>
                    {enviando ? 'Registrando…' : 'Registrar venta y emitir boleta'}
                  </button>
                </>
              )}
            </aside>
          </div>

          <form className="tarjeta cajero-cierre cajero-panel-arqueo" onSubmit={cerrar}>
            <div><h3>Cerrar turno y realizar arqueo</h3><p>Cuenta el efectivo físico e ingresa el total contado para comparar con el efectivo esperado.</p></div>
            <div className="cajero-cierre-campos">
              <label htmlFor="monto-contado">Efectivo contado (S/)</label>
              <input id="monto-contado" type="number" min="0" step="0.01" required value={montoContado} onChange={(e) => setMontoContado(e.target.value)} placeholder="0.00" />
              <label htmlFor="observacion-cierre">Observación (opcional)</label>
              <input id="observacion-cierre" maxLength="200" value={observacion} onChange={(e) => setObservacion(e.target.value)} placeholder="Anota alguna diferencia o incidencia" />
              <button className="boton boton-peligro" type="submit" disabled={enviando || montoContado === ''}>{enviando ? 'Cerrando…' : 'Cerrar caja'}</button>
            </div>
          </form>
        </>
      ) : (
        <div className="tarjeta cajero-apertura cajero-panel-apertura">
          <div className="cajero-apertura-icono"><Wallet size={32} /></div>
          <h3>Inicia tu turno</h3>
          <p>No tienes una caja abierta. Registra el monto de efectivo con el que empiezas para habilitar los cobros.</p>
          <form onSubmit={abrir}>
            <label htmlFor="monto-inicial">Monto inicial de caja (S/)</label>
            <input id="monto-inicial" type="number" min="0" step="0.01" required value={montoInicial} onChange={(e) => setMontoInicial(e.target.value)} placeholder="Ej. 100.00" />
            <button className="boton" type="submit" disabled={enviando || montoInicial === ''}>{enviando ? 'Abriendo…' : 'Abrir caja y comenzar turno'}</button>
          </form>
        </div>
      )}

      {ultima && <p className="aviso" role="status">Venta registrada: pedido {ultima.pedidoCodigo} por {soles(ultima.total)}. Boleta {ultima.comprobanteNumero}.{' '}
        <button className="enlace" onClick={() => verBoleta(ultima.comprobanteId)}>Ver boleta (PDF)</button></p>}

      {ultimoCierre && <div className="aviso cajero-cierre-resultado" role="status">
        <strong>Turno cerrado correctamente.</strong>
        <span>Efectivo esperado: {soles(ultimoCierre.efectivoEsperado)} · Contado: {soles(ultimoCierre.montoContado)} · Diferencia: {soles(ultimoCierre.diferencia)}</span>
      </div>}

      <div className="tarjeta cajero-historial cajero-panel-historial">
        <div className="tarjeta-cabecera"><h3>Ventas recientes</h3><span className="nota">Últimos movimientos</span></div>
        <div className="contenedor-tabla">
          <table className="tabla">
            <thead><tr><th>Hora</th><th>Pedido</th><th>Mesa</th><th>Método de pago</th><th>Total</th><th>Boleta</th></tr></thead>
            <tbody>
              {recientes.map((v) => <tr key={v.id}>
                <td>{hora(v.fechaHora)}</td><td>{v.pedidoCodigo}</td><td>{v.mesa}</td><td>{v.metodoPago}</td><td>{soles(v.total)}</td>
                <td><button className="enlace" onClick={() => verBoleta(v.comprobanteId ?? v.id)} hidden={!v.comprobanteId}>Ver PDF</button></td>
              </tr>)}
              {recientes.length === 0 && <tr><td colSpan={6} className="nota">Aún no hay ventas registradas.</td></tr>}
            </tbody>
          </table>
        </div>
      </div>
    </section>
  );
}
