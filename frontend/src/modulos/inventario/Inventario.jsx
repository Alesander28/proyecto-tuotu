import { useCallback, useEffect, useState } from 'react';
import { api } from '../../comun/api.js';
import './inventario.css';

// HU-011 (entradas), HU-012 (salidas) y HU-013 (consulta del inventario).
export default function Inventario() {
  const [insumos, setInsumos] = useState([]);
  const [texto, setTexto] = useState('');
  const [error, setError] = useState('');
  const [aviso, setAviso] = useState('');
  const [movimiento, setMovimiento] = useState(null); // { insumo, tipo }
  const [datos, setDatos] = useState({ cantidad: '', motivo: '' });
  const [historial, setHistorial] = useState(null); // { insumo, lista }
  const [nuevo, setNuevo] = useState(null); // { nombre, unidadMedida }

  const cargar = useCallback(async () => {
    try {
      setInsumos(await api('/insumos?q=' + encodeURIComponent(texto)));
      setError('');
    } catch (e) {
      setError(e.message);
    }
  }, [texto]);

  useEffect(() => {
    cargar();
  }, [cargar]);

  function abrirMovimiento(insumo, tipo) {
    setMovimiento({ insumo, tipo });
    setDatos({ cantidad: '', motivo: '' });
    setError('');
    setAviso('');
  }

  async function guardarMovimiento(evento) {
    evento.preventDefault();
    setError('');
    try {
      const actualizado = await api(`/insumos/${movimiento.insumo.id}/${movimiento.tipo}`, {
        metodo: 'POST',
        cuerpo: { cantidad: Number(datos.cantidad), motivo: datos.motivo },
      });
      setAviso(`${movimiento.tipo === 'entradas' ? 'Entrada' : 'Salida'} registrada. Stock actual de ${actualizado.nombre}: ${actualizado.stockActual} ${actualizado.unidadMedida}.`);
      setMovimiento(null);
      await cargar();
    } catch (e) {
      setError(e.message);
    }
  }

  async function verHistorial(insumo) {
    try {
      setHistorial({ insumo, lista: await api(`/insumos/${insumo.id}/movimientos`) });
    } catch (e) {
      setError(e.message);
    }
  }

  async function guardarNuevo(evento) {
    evento.preventDefault();
    setError('');
    try {
      await api('/insumos', { metodo: 'POST', cuerpo: nuevo });
      setAviso('Insumo registrado. Registre su primera entrada para cargar el stock.');
      setNuevo(null);
      await cargar();
    } catch (e) {
      setError(e.message);
    }
  }

  const fecha = (f) => new Date(f).toLocaleString('es-PE', { dateStyle: 'short', timeStyle: 'short' });

  return (
    <div>
      <div className="fila-titulo">
        <h2>Inventario de insumos</h2>
        <button className="boton" onClick={() => setNuevo({ nombre: '', unidadMedida: 'kg' })}>+ Nuevo insumo</button>
      </div>

      <label htmlFor="buscar" className="solo-lectores">Buscar insumo</label>
      <input id="buscar" className="buscador" placeholder="Buscar insumo…" value={texto}
             onChange={(e) => setTexto(e.target.value)} />

      {error && <p className="error" role="alert">{error}</p>}
      {aviso && <p className="aviso" role="status">{aviso}</p>}

      {nuevo && (
        <form className="tarjeta formulario" onSubmit={guardarNuevo}>
          <h3>Nuevo insumo</h3>
          <label htmlFor="n-nombre">Nombre</label>
          <input id="n-nombre" value={nuevo.nombre} onChange={(e) => setNuevo({ ...nuevo, nombre: e.target.value })} required />
          <label htmlFor="n-unidad">Unidad de medida</label>
          <input id="n-unidad" value={nuevo.unidadMedida} onChange={(e) => setNuevo({ ...nuevo, unidadMedida: e.target.value })} required />
          <div className="acciones">
            <button className="boton" type="submit">Guardar</button>
            <button className="boton secundario" type="button" onClick={() => setNuevo(null)}>Cancelar</button>
          </div>
        </form>
      )}

      {movimiento && (
        <form className="tarjeta formulario" onSubmit={guardarMovimiento}>
          <h3>{movimiento.tipo === 'entradas' ? 'Registrar entrada' : 'Registrar salida'}: {movimiento.insumo.nombre}</h3>
          <label htmlFor="m-cantidad">Cantidad ({movimiento.insumo.unidadMedida})</label>
          <input id="m-cantidad" type="number" step="0.01" min="0.01" value={datos.cantidad}
                 onChange={(e) => setDatos({ ...datos, cantidad: e.target.value })} required />
          <label htmlFor="m-motivo">
            {movimiento.tipo === 'entradas' ? 'Nota (opcional)' : 'Motivo (consumo, merma u otro)'}
          </label>
          <input id="m-motivo" value={datos.motivo} onChange={(e) => setDatos({ ...datos, motivo: e.target.value })}
                 required={movimiento.tipo === 'salidas'} maxLength={120} />
          <div className="acciones">
            <button className="boton" type="submit">Guardar</button>
            <button className="boton secundario" type="button" onClick={() => setMovimiento(null)}>Cancelar</button>
          </div>
        </form>
      )}

      <table className="tabla">
        <thead>
          <tr><th>Insumo</th><th>Unidad</th><th>Stock actual</th><th>Acciones</th></tr>
        </thead>
        <tbody>
          {insumos.map((i) => (
            <tr key={i.id}>
              <td>{i.nombre}</td>
              <td>{i.unidadMedida}</td>
              <td><strong>{Number(i.stockActual).toFixed(2)}</strong></td>
              <td>
                <button className="enlace" onClick={() => abrirMovimiento(i, 'entradas')}>Registrar entrada</button>
                <button className="enlace peligro" onClick={() => abrirMovimiento(i, 'salidas')}>Registrar salida</button>
                <button className="enlace" onClick={() => verHistorial(i)}>Historial</button>
              </td>
            </tr>
          ))}
          {insumos.length === 0 && <tr><td colSpan={4} className="nota">No hay insumos que coincidan.</td></tr>}
        </tbody>
      </table>

      {historial && (
        <div className="tarjeta historial">
          <div className="fila-titulo">
            <h3>Movimientos de {historial.insumo.nombre}</h3>
            <button className="boton secundario" onClick={() => setHistorial(null)}>Cerrar</button>
          </div>
          {historial.lista.length === 0 && <p className="nota">Sin movimientos.</p>}
          <ul>
            {historial.lista.map((m) => (
              <li key={m.id}>
                {fecha(m.fechaHora)} · <strong>{m.tipo === 'ENTRADA' ? 'Entrada' : 'Salida'}</strong> de {Number(m.cantidad).toFixed(2)} ·
                {' '}{m.motivo} · {m.responsable}
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
