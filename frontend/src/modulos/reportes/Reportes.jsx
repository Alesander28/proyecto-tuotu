import { useCallback, useEffect, useState } from 'react';
import { api } from '../../comun/api.js';
import { soles } from '../../comun/formato.js';
import './reportes.css';

function hoy() {
  const f = new Date();
  const dos = (n) => String(n).padStart(2, '0');
  return `${f.getFullYear()}-${dos(f.getMonth() + 1)}-${dos(f.getDate())}`;
}

// HU-015: reportes de ventas y de inventario para el gerente.
export default function Reportes() {
  const [desde, setDesde] = useState(hoy());
  const [hasta, setHasta] = useState(hoy());
  const [ventas, setVentas] = useState(null);
  const [inventario, setInventario] = useState(null);
  const [error, setError] = useState('');

  const generar = useCallback(async () => {
    setError('');
    const consulta = `?desde=${desde}&hasta=${hasta}`;
    try {
      const [v, i] = await Promise.all([api('/reportes/ventas' + consulta), api('/reportes/inventario' + consulta)]);
      setVentas(v);
      setInventario(i);
    } catch (e) {
      setError(e.message);
    }
  }, [desde, hasta]);

  useEffect(() => {
    generar();
  }, []);

  const maximo = (lista) => Math.max(1, ...lista.map((x) => Number(x.monto ?? x.cantidad)));

  return (
    <div>
      <div className="fila-titulo">
        <h2>Reportes de ventas e inventario</h2>
        <button className="boton secundario no-imprimir" onClick={() => window.print()}>Imprimir / guardar como PDF</button>
      </div>

      <form className="fila-formulario no-imprimir" onSubmit={(e) => { e.preventDefault(); generar(); }}>
        <label htmlFor="desde">Desde</label>
        <input id="desde" type="date" value={desde} max={hasta} onChange={(e) => setDesde(e.target.value)} required />
        <label htmlFor="hasta">Hasta</label>
        <input id="hasta" type="date" value={hasta} min={desde} onChange={(e) => setHasta(e.target.value)} required />
        <button className="boton" type="submit">Generar</button>
      </form>

      {error && <p className="error" role="alert">{error}</p>}

      {ventas && (
        <>
          <div className="resumen-cifras">
            <div className="tarjeta"><span>Ventas</span><strong data-testid="cantidad-ventas">{ventas.cantidadVentas}</strong></div>
            <div className="tarjeta"><span>Total vendido</span><strong data-testid="total-vendido">{soles(ventas.totalVendido)}</strong></div>
          </div>

          <div className="reportes-columnas">
            <section className="tarjeta" aria-label="Ventas por categoría">
              <h3>Ventas por categoría</h3>
              {ventas.porCategoria.length === 0 && <p className="nota">Sin ventas en este periodo.</p>}
              {ventas.porCategoria.map((c) => (
                <div key={c.nombre} className="barra-fila">
                  <span>{c.nombre}</span>
                  <div className="barra"><div style={{ width: `${(Number(c.monto) / maximo(ventas.porCategoria)) * 100}%` }} /></div>
                  <strong>{soles(c.monto)}</strong>
                </div>
              ))}
            </section>

            <section className="tarjeta" aria-label="Productos más vendidos">
              <h3>Productos más vendidos</h3>
              {ventas.masVendidos.length === 0 && <p className="nota">Sin ventas en este periodo.</p>}
              <ol>
                {ventas.masVendidos.map((p) => <li key={p.nombre}>{p.nombre}: <strong>{p.cantidad}</strong> unidades</li>)}
              </ol>
              <h3>Por método de pago</h3>
              <ul>
                {ventas.porMetodoPago.map((m) => <li key={m.nombre}>{m.nombre}: <strong>{soles(m.monto)}</strong></li>)}
              </ul>
            </section>
          </div>
        </>
      )}

      {inventario && (
        <section aria-label="Inventario">
          <h3>Inventario del periodo</h3>
          <table className="tabla">
            <thead>
              <tr><th>Insumo</th><th>Unidad</th><th>Entradas</th><th>Salidas</th><th>Stock actual</th></tr>
            </thead>
            <tbody>
              {inventario.insumos.map((i) => (
                <tr key={i.insumo}>
                  <td>{i.insumo}</td>
                  <td>{i.unidad}</td>
                  <td>{Number(i.entradas).toFixed(2)}</td>
                  <td>{Number(i.salidas).toFixed(2)}</td>
                  <td><strong>{Number(i.stockActual).toFixed(2)}</strong></td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      )}
    </div>
  );
}
