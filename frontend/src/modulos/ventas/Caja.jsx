import { useCallback, useEffect, useState } from 'react';
import { abrirPdf, api } from '../../comun/api.js';
import { soles } from '../../comun/formato.js';
import './ventas.css';

// HU-009 (registrar ventas) y HU-014 (boleta de consumo en PDF).
export default function Caja() {
  const [pendientes, setPendientes] = useState([]);
  const [metodos, setMetodos] = useState([]);
  const [recientes, setRecientes] = useState([]);
  const [elegido, setElegido] = useState(null);
  const [metodo, setMetodo] = useState('');
  const [ultima, setUltima] = useState(null);
  const [error, setError] = useState('');
  const [enviando, setEnviando] = useState(false);

  // limpiarError = false cuando se recarga después de un error, para que el mensaje no desaparezca.
  const cargar = useCallback(async (limpiarError = true) => {
    try {
      const [p, m, r] = await Promise.all([api('/ventas/pendientes'), api('/ventas/metodos-pago'), api('/ventas')]);
      setPendientes(p);
      setMetodos(m);
      setRecientes(r.slice(0, 10));
      setMetodo((actual) => actual || m[0] || '');
      if (limpiarError !== false) setError('');
    } catch (e) {
      setError(e.message);
    }
  }, []);

  useEffect(() => {
    cargar();
    const reloj = setInterval(cargar, 15000);
    return () => clearInterval(reloj);
  }, [cargar]);

  async function registrar() {
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

  const hora = (f) => new Date(f).toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' });

  return (
    <div>
      <div className="fila-titulo">
        <h2>Cobrar un pedido atendido</h2>
        <button className="boton secundario" onClick={cargar}>Actualizar</button>
      </div>
      {error && <p className="error" role="alert">{error}</p>}
      {ultima && (
        <p className="aviso" role="status">
          Venta registrada: pedido {ultima.pedidoCodigo} por {soles(ultima.total)}. Boleta {ultima.comprobanteNumero}.{' '}
          <button className="enlace" onClick={() => verBoleta(ultima.comprobanteId)}>Ver boleta (PDF)</button>
        </p>
      )}

      <div className="caja-pantalla">
        <section aria-label="Pedidos atendidos">
          <h3>Pendientes de cobro ({pendientes.length})</h3>
          {pendientes.length === 0 && <p className="nota">No hay pedidos atendidos por cobrar.</p>}
          {pendientes.map((p) => (
            <button key={p.id} className={elegido?.id === p.id ? 'pedido-cobro elegido' : 'pedido-cobro'}
                    aria-label={`Seleccionar pedido ${p.codigo}`} onClick={() => setElegido(p)}>
              <span><strong>Pedido {p.codigo}</strong> · Mesa {p.mesa}</span>
              <span>{soles(p.total)}</span>
            </button>
          ))}
        </section>

        <aside className="tarjeta resumen" aria-label="Detalle de la venta">
          <h3>Detalle de la venta</h3>
          {!elegido && <p className="nota">Elija un pedido de la lista.</p>}
          {elegido && (
            <>
              <p>Pedido {elegido.codigo} · Mesa {elegido.mesa} · Atendió {elegido.mozo}</p>
              {elegido.detalles.map((d, i) => (
                <div key={i} className="linea-titulo">
                  <span>
                    {d.cantidad} × {d.producto} ({d.presentacion}){d.extraQueso ? ' + extra queso' : ''}{d.paraLlevar ? ' · para llevar' : ''}
                  </span>
                  <span>{soles(d.subtotal)}</span>
                </div>
              ))}
              <div className="total"><span>Total</span><strong data-testid="total-venta">{soles(elegido.total)}</strong></div>
              <label htmlFor="metodo">Método de pago</label>
              <select id="metodo" value={metodo} onChange={(e) => setMetodo(e.target.value)}>
                {metodos.map((m) => <option key={m} value={m}>{m}</option>)}
              </select>
              <button className="boton" onClick={registrar} disabled={enviando}>
                {enviando ? 'Registrando…' : 'Registrar venta y emitir boleta'}
              </button>
            </>
          )}
        </aside>
      </div>

      <h3>Ventas recientes</h3>
      <table className="tabla">
        <thead>
          <tr><th>Hora</th><th>Pedido</th><th>Mesa</th><th>Método</th><th>Total</th><th>Boleta</th></tr>
        </thead>
        <tbody>
          {recientes.map((v) => (
            <tr key={v.id}>
              <td>{hora(v.fechaHora)}</td>
              <td>{v.pedidoCodigo}</td>
              <td>{v.mesa}</td>
              <td>{v.metodoPago}</td>
              <td>{soles(v.total)}</td>
              <td><button className="enlace" onClick={() => verBoleta(v.comprobanteId ?? v.id)} hidden={!v.comprobanteId}>Ver PDF</button></td>
            </tr>
          ))}
          {recientes.length === 0 && <tr><td colSpan={6} className="nota">Aún no hay ventas.</td></tr>}
        </tbody>
      </table>
    </div>
  );
}
