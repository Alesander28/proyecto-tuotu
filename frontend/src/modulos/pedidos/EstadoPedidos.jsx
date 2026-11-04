import { useCallback, useEffect, useState } from 'react';
import { api } from '../../comun/api.js';
import { useAuth } from '../seguridad/auth.jsx';
import { soles } from '../../comun/formato.js';
import './pedidos.css';

const COLUMNAS = [
  { estado: 'PENDIENTE', titulo: 'Pendiente', siguiente: 'EN_PREPARACION', boton: 'Pasar a preparación' },
  { estado: 'EN_PREPARACION', titulo: 'En preparación', siguiente: 'ATENDIDO', boton: 'Marcar atendido' },
  { estado: 'ATENDIDO', titulo: 'Atendido', siguiente: null, boton: null },
];

// HU-008: seguimiento de los pedidos en tres columnas. Se actualiza solo cada 10 segundos.
export default function EstadoPedidos() {
  const { tienePermiso } = useAuth();
  const [pedidos, setPedidos] = useState([]);
  const [error, setError] = useState('');

  const cargar = useCallback(async () => {
    try {
      setPedidos(await api('/pedidos'));
      setError('');
    } catch (e) {
      setError(e.message);
    }
  }, []);

  useEffect(() => {
    cargar();
    const reloj = setInterval(cargar, 10000);
    return () => clearInterval(reloj);
  }, [cargar]);

  async function avanzar(pedido, siguiente) {
    try {
      await api(`/pedidos/${pedido.id}/estado`, { metodo: 'PATCH', cuerpo: { estado: siguiente } });
      await cargar();
    } catch (e) {
      setError(e.message);
    }
  }

  const hora = (f) => new Date(f).toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' });

  return (
    <div>
      <div className="fila-titulo">
        <h2>Estado de los pedidos</h2>
        <button className="boton secundario" onClick={cargar}>Actualizar</button>
      </div>
      {error && <p className="error" role="alert">{error}</p>}

      <div className="columnas">
        {COLUMNAS.map((col) => {
          const lista = pedidos.filter((p) => p.estado === col.estado);
          return (
            <section key={col.estado} className={`columna ${col.estado.toLowerCase()}`} aria-label={col.titulo}>
              <h3>{col.titulo} ({lista.length})</h3>
              {lista.length === 0 && <p className="nota">Sin pedidos.</p>}
              {lista.map((p) => (
                <article key={p.id} className="tarjeta-pedido">
                  <div className="linea-titulo">
                    <strong>Pedido {p.codigo}</strong>
                    <span>{hora(p.fechaHora)}</span>
                  </div>
                  <div>Mesa {p.mesa} · {p.mozo}</div>
                  <ul>
                    {p.detalles.map((d, i) => (
                      <li key={i}>
                        {d.cantidad} × {d.producto} ({d.presentacion})
                        {d.extraQueso ? ' + extra queso' : ''}{d.paraLlevar ? ' · para llevar' : ''}
                      </li>
                    ))}
                  </ul>
                  <div className="linea-titulo"><span>Total</span><strong>{soles(p.total)}</strong></div>
                  {col.siguiente && tienePermiso('PEDIDOS_ESTADO') && (
                    <button className="boton ligero" onClick={() => avanzar(p, col.siguiente)}>{col.boton}</button>
                  )}
                </article>
              ))}
            </section>
          );
        })}
      </div>
    </div>
  );
}
