import { useCallback, useEffect, useMemo, useState } from 'react';
import { AlertTriangle, ArrowDownToLine, ArrowUpFromLine, Boxes, Clock3, History, Package, Plus, SlidersHorizontal, Warehouse } from 'lucide-react';
import { api } from '../../comun/api.js';
import { exportarCsv } from '../../comun/exportar.js';
import { CampoBusqueda, EncabezadoPagina, InsigniaStock, Modal, Vacio } from '../../comun/ui.jsx';
import './inventario.css';

const numero = (v) => Number(v ?? 0).toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const fecha = (f) => f ? new Date(f).toLocaleString('es-PE', { dateStyle: 'short', timeStyle: 'short' }) : '—';
const tonoStock = (estado) => { const colores = { AGOTADO: 'agotado', BAJO: 'bajo', PROXIMO: 'proximo', NORMAL: 'normal' }; return colores[estado] ?? 'normal'; };
const estadoLabel = { NORMAL: 'Disponible', PROXIMO: 'Próximo a agotarse', BAJO: 'Stock bajo', AGOTADO: 'Agotado' };

export default function Inventario() {
  const [insumos, setInsumos] = useState(null);
  const [movimientosRecientes, setMovimientosRecientes] = useState([]);
  const [texto, setTexto] = useState('');
  const [filtroEstado, setFiltroEstado] = useState('');
  const [error, setError] = useState('');
  const [aviso, setAviso] = useState('');
  const [modal, setModal] = useState(null);
  const [datos, setDatos] = useState({ cantidad: '', motivo: '' });
  const [enviando, setEnviando] = useState(false);

  const cargar = useCallback(async () => {
    try {
      const [lista, movs] = await Promise.all([api('/insumos?q=' + encodeURIComponent(texto)), api('/insumos/movimientos')]);
      setInsumos(lista); setMovimientosRecientes(Array.isArray(movs) ? movs.filter(m => m && m.insumo) : []); setError('');
    } catch (e) { setError(e.message); }
  }, [texto]);
  useEffect(() => { cargar(); }, [cargar]);

  const resumen = useMemo(() => {
    const lista = insumos ?? [];
    return { total: lista.length, agotados: lista.filter(i => i.estadoStock === 'AGOTADO').length,
      bajos: lista.filter(i => i.estadoStock === 'BAJO').length, proximos: lista.filter(i => i.estadoStock === 'PROXIMO').length,
      normales: lista.filter(i => i.estadoStock === 'NORMAL').length };
  }, [insumos]);
  const visibles = (insumos ?? []).filter(i => !filtroEstado || i.estadoStock === filtroEstado);

  async function ejecutar(accion, mensaje) {
    setEnviando(true); setError('');
    try { await accion(); setAviso(mensaje); setModal(null); await cargar(); }
    catch (e) { setError(e.message); }
    finally { setEnviando(false); }
  }
  function abrirMovimiento(insumo, tipo) { setDatos({ cantidad: '', motivo: '' }); setModal({ tipo: 'movimiento', insumo, movimiento: tipo }); }
  function guardarMovimiento(e) {
    e.preventDefault();
    ejecutar(() => api(`/insumos/${modal.insumo.id}/${modal.movimiento}`, { metodo: 'POST', cuerpo: { cantidad: Number(datos.cantidad), motivo: datos.motivo } }), `${modal.movimiento === 'entradas' ? 'Entrada' : 'Salida'} registrada correctamente.`);
  }
  function guardarNuevo(e) {
    e.preventDefault();
    ejecutar(() => api('/insumos', { metodo: 'POST', cuerpo: { ...modal.datos, stockMinimo: Number(modal.datos.stockMinimo) } }), 'Insumo registrado. Ahora puedes ingresar su stock inicial.');
  }
  function guardarMinimo(e) {
    e.preventDefault();
    ejecutar(() => api(`/insumos/${modal.insumo.id}/stock-minimo`, { metodo: 'PUT', cuerpo: { stockMinimo: Number(modal.datos.stockMinimo) } }), 'Stock mínimo actualizado.');
  }
  async function verHistorial(insumo) {
    setError('');
    try { const lista = await api(`/insumos/${insumo.id}/movimientos`); setModal({ tipo: 'historial', insumo, lista }); }
    catch (e) { setError(e.message); }
  }

  return <div className="inventario-moderno">
    <EncabezadoPagina titulo="Inventario" descripcion="Controla las existencias, registra movimientos y anticipa reposiciones.">
      <button type="button" className="boton" onClick={() => setModal({ tipo: 'nuevo', datos: { nombre: '', unidadMedida: 'kg', stockMinimo: '0' } })}><Plus size={16}/> Nuevo insumo</button>
      <button type="button" className="boton secundario" disabled={!visibles.length} onClick={() => exportarCsv('inventario', [
        { titulo: 'Insumo', valor: i => i.nombre }, { titulo: 'Unidad', valor: i => i.unidadMedida },
        { titulo: 'Stock actual', valor: i => i.stockActual }, { titulo: 'Stock mínimo', valor: i => i.stockMinimo },
        { titulo: 'Estado', valor: i => estadoLabel[i.estadoStock] ?? i.estadoStock }], visibles)}><Warehouse size={16}/> Exportar CSV</button>
    </EncabezadoPagina>
    {error && <p className="error" role="alert">{error}</p>}{aviso && <p className="aviso" role="status">{aviso}</p>}

    {insumos && <section className="inv-kpis" aria-label="Resumen de inventario">
      <article className="inv-kpi"><span className="inv-kpi-icon naranja"><Boxes size={21}/></span><div><small>Insumos registrados</small><strong>{resumen.total}</strong><span className="inv-kpi-note">En almacén</span></div></article>
      <article className="inv-kpi"><span className="inv-kpi-icon verde"><Package size={21}/></span><div><small>Stock normal</small><strong>{resumen.normales}</strong><span className="inv-kpi-note">Disponibles</span></div></article>
      <article className="inv-kpi"><span className="inv-kpi-icon ambar"><AlertTriangle size={21}/></span><div><small>Por reponer</small><strong>{resumen.bajos + resumen.proximos}</strong><span className="inv-kpi-note">Bajo o próximo a agotarse</span></div></article>
      <article className="inv-kpi"><span className="inv-kpi-icon rojo"><Warehouse size={21}/></span><div><small>Agotados</small><strong>{resumen.agotados}</strong><span className="inv-kpi-note">Necesitan reposición</span></div></article>
    </section>}

    <section className="inv-filtros">
      <CampoBusqueda id="inv-buscar" etiqueta="Buscar insumo" placeholder="Nombre del insumo…" valor={texto} onChange={setTexto}/>
      <div><label htmlFor="inv-estado"><SlidersHorizontal size={14} className="inv-label-icon"/> Estado de stock</label><select id="inv-estado" value={filtroEstado} onChange={e => setFiltroEstado(e.target.value)}>
        <option value="">Todos los estados</option><option value="NORMAL">Disponible</option><option value="PROXIMO">Próximo a agotarse</option><option value="BAJO">Stock bajo</option><option value="AGOTADO">Agotado</option>
      </select></div>
    </section>

    <section className="inv-panel">
      <div className="inv-panel-head"><div><h3>Existencias de insumos</h3><p>Consulta el stock y gestiona entradas o salidas.</p></div><span className="inv-count">{visibles.length} insumo(s)</span></div>
      {!insumos ? <div className="inv-loading">Cargando inventario…</div> : visibles.length === 0 ? <Vacio icono={Warehouse} texto="No hay insumos que coincidan con los filtros."/> :
      <div className="inv-table-wrap"><table className="tabla inv-tabla"><thead><tr><th>Insumo</th><th>Stock actual</th><th>Stock mínimo</th><th>Estado</th><th>Acciones</th></tr></thead><tbody>
        {visibles.map(i => <tr key={i.id}>
          <td><div className="inv-name"><span className={`inv-mini-icon ${tonoStock(i.estadoStock)}`}><Package size={18}/></span><div><strong>{i.nombre}</strong><small>Unidad: {i.unidadMedida}</small></div></div></td>
          <td><div className="inv-stock"><strong className={tonoStock(i.estadoStock)}>{numero(i.stockActual)}</strong><small>{i.unidadMedida}</small></div>{i.stockMinimo > 0 && <div className="inv-progress"><span className={tonoStock(i.estadoStock)} style={{width: `${Math.min(100, Math.max(4, Number(i.stockActual) / Number(i.stockMinimo) * 60))}%`}}/></div>}</td>
          <td>{numero(i.stockMinimo)} <small>{i.unidadMedida}</small></td><td><InsigniaStock estado={i.estadoStock}/></td>
          <td><div className="inv-actions"><button type="button" className="inv-action entrada" onClick={() => abrirMovimiento(i, 'entradas')} title="Registrar entrada" aria-label="Registrar entrada"><ArrowDownToLine size={15}/> Entrada</button><button type="button" className="inv-action salida" onClick={() => abrirMovimiento(i, 'salidas')} title="Registrar salida" aria-label="Registrar salida"><ArrowUpFromLine size={15}/> Salida</button><button type="button" className="inv-action" onClick={() => setModal({ tipo: 'minimo', insumo: i, datos: { stockMinimo: String(i.stockMinimo ?? 0) } })} title="Configurar stock mínimo"><SlidersHorizontal size={15}/></button><button type="button" className="inv-action" onClick={() => verHistorial(i)} title="Ver historial"><History size={15}/></button></div></td>
        </tr>)}
      </tbody></table></div>}
    </section>

    <section className="inv-panel inv-movimientos-panel"><div className="inv-panel-head"><div><h3><Clock3 size={18}/> Movimientos recientes</h3><p>Entradas y salidas registradas en los últimos 30 días.</p></div></div>
      {!insumos ? <div className="inv-loading">Cargando movimientos…</div> : movimientosRecientes.length === 0 ? <Vacio icono={History} texto="Todavía no hay movimientos registrados."/> :
      <div className="inv-mov-lista">{movimientosRecientes.slice(0, 8).map(m => <div className="inv-mov-row" key={m.id}><span className={`inv-mov-icon ${m.tipo === 'ENTRADA' ? 'entrada' : 'salida'}`}>{m.tipo === 'ENTRADA' ? <ArrowDownToLine size={17}/> : <ArrowUpFromLine size={17}/>}</span><div className="inv-mov-info"><strong>{m.insumo}</strong><small>{fecha(m.fechaHora)} · {m.responsable || 'Usuario'}{m.motivo ? ` · ${m.motivo}` : ''}</small></div><span className={`inv-mov-cantidad ${m.tipo === 'ENTRADA' ? 'entrada' : 'salida'}`}>{m.tipo === 'ENTRADA' ? '+' : '−'}{numero(m.cantidad)} {m.unidad}</span></div>)}</div>}
    </section>

    {modal?.tipo === 'nuevo' && <Modal titulo="Registrar nuevo insumo" onCerrar={() => setModal(null)}><form className="inv-form" onSubmit={guardarNuevo}><label>Nombre del insumo</label><input autoFocus maxLength={80} value={modal.datos.nombre} onChange={e => setModal({...modal, datos:{...modal.datos, nombre:e.target.value}})} placeholder="Ej. Mozzarella" required/><label>Unidad de medida</label><select value={modal.datos.unidadMedida} onChange={e => setModal({...modal, datos:{...modal.datos, unidadMedida:e.target.value}})}><option>kg</option><option>g</option><option>l</option><option>ml</option><option>unidad</option><option>paquete</option><option>caja</option><option>bolsa</option></select><label>Stock mínimo (alerta de reposición)</label><input type="number" min="0" step="0.01" value={modal.datos.stockMinimo} onChange={e => setModal({...modal, datos:{...modal.datos, stockMinimo:e.target.value}})}/><p className="inv-hint">El insumo se registra inicialmente con stock 0. Luego utiliza «Entrada» para registrar la cantidad recibida.</p><div className="acciones"><button className="boton" disabled={enviando}>{enviando?'Guardando…':'Guardar insumo'}</button><button type="button" className="boton secundario" onClick={() => setModal(null)}>Cancelar</button></div></form></Modal>}
    {modal?.tipo === 'movimiento' && <Modal titulo={modal.movimiento === 'entradas' ? 'Registrar entrada de stock' : 'Registrar salida de stock'} onCerrar={() => setModal(null)}><form className="inv-form" onSubmit={guardarMovimiento}><div className="inv-modal-insumo"><Package size={19}/><div><strong>{modal.insumo.nombre}</strong><small>Stock actual: {numero(modal.insumo.stockActual)} {modal.insumo.unidadMedida}</small></div></div><label>Cantidad ({modal.insumo.unidadMedida})</label><input autoFocus type="number" min="0.01" step="0.01" value={datos.cantidad} onChange={e => setDatos({...datos, cantidad:e.target.value})} required/><label>{modal.movimiento === 'entradas' ? 'Nota (opcional)' : 'Motivo de salida'}</label><input maxLength={120} value={datos.motivo} onChange={e => setDatos({...datos, motivo:e.target.value})} placeholder={modal.movimiento === 'entradas' ? 'Compra, proveedor…' : 'Consumo, merma, vencimiento…'} required={modal.movimiento === 'salidas'}/><div className="acciones"><button className="boton" disabled={enviando}>{enviando?'Registrando…':'Guardar'}</button><button type="button" className="boton secundario" onClick={() => setModal(null)}>Cancelar</button></div></form></Modal>}
    {modal?.tipo === 'minimo' && <Modal titulo="Configurar alerta de stock" onCerrar={() => setModal(null)} estrecho><form className="inv-form" onSubmit={guardarMinimo}><div className="inv-modal-insumo"><AlertTriangle size={19}/><div><strong>{modal.insumo.nombre}</strong><small>Stock actual: {numero(modal.insumo.stockActual)} {modal.insumo.unidadMedida}</small></div></div><label>Alertar cuando el stock sea igual o menor a</label><input type="number" min="0" step="0.01" value={modal.datos.stockMinimo} onChange={e => setModal({...modal, datos:{...modal.datos, stockMinimo:e.target.value}})} required/><small>Unidad: {modal.insumo.unidadMedida}. Coloca 0 para desactivar la alerta de mínimo.</small><div className="acciones"><button className="boton" disabled={enviando}>{enviando?'Guardando…':'Guardar mínimo'}</button><button type="button" className="boton secundario" onClick={() => setModal(null)}>Cancelar</button></div></form></Modal>}
    {modal?.tipo === 'historial' && <Modal titulo={`Movimientos · ${modal.insumo.nombre}`} onCerrar={() => setModal(null)}><div className="inv-history-meta">Stock actual: <strong>{numero(modal.insumo.stockActual)} {modal.insumo.unidadMedida}</strong></div>{modal.lista.length === 0 ? <Vacio icono={History} texto="Este insumo todavía no tiene movimientos."/> : <div className="inv-history-list">{modal.lista.map(m => <article key={m.id} className="inv-history-item"><span className={`inv-mov-icon ${m.tipo === 'ENTRADA' ? 'entrada' : 'salida'}`}>{m.tipo === 'ENTRADA' ? <ArrowDownToLine size={16}/> : <ArrowUpFromLine size={16}/>}</span><div><strong>{m.tipo === 'ENTRADA' ? 'Entrada' : 'Salida'} · {numero(m.cantidad)} {modal.insumo.unidadMedida}</strong><small>{fecha(m.fechaHora)} · {m.responsable || 'Usuario'}</small><p>{m.motivo || 'Sin nota'}</p></div></article>)}</div>}</Modal>}
  </div>;
}
