import { useCallback, useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { ClipboardList, Download, Eye, Printer } from 'lucide-react';
import { api } from '../../comun/api.js';
import { exportarCsv, imprimir } from '../../comun/exportar.js';
import { ETIQUETA_ESTADO_PEDIDO, fechaHora, soles } from '../../comun/formato.js';
import { aIso, consulta } from '../../comun/periodos.js';
import { CampoBusqueda, EncabezadoPagina, Esqueleto, FiltroPeriodo, InsigniaPedido, Modal, Vacio, periodoInicial } from '../../comun/ui.jsx';
import './gerente.css';

const ETIQUETA_HISTORIAL = { PEDIDO_REGISTRADO: 'Pedido registrado', CAMBIO_ESTADO_PEDIDO: 'Cambio de estado', VENTA_REGISTRADA: 'Venta registrada' };

// Consulta de pedidos para el gerente. Solo lectura: registrar y cambiar el estado corresponde al mozo.
export default function PedidosGerente() {
  const [parametros, setParametros] = useSearchParams();
  const [periodo, setPeriodo] = useState(periodoInicial('mes'));
  const [estado, setEstado] = useState('');
  const [mozo, setMozo] = useState('');
  const [texto, setTexto] = useState('');
  const [pedidos, setPedidos] = useState(null);
  const [error, setError] = useState('');
  const [detalle, setDetalle] = useState(null);

  const cargar = useCallback(async () => {
    if (periodo.hasta < periodo.desde) return;
    try {
      setPedidos(await api('/pedidos/buscar' + consulta({ desde: periodo.desde, hasta: periodo.hasta, estado, mozo, q: texto })));
      setError('');
    } catch (e) {
      setError(e.message);
    }
  }, [periodo, estado, mozo, texto]);

  useEffect(() => {
    const espera = setTimeout(cargar, 300);
    return () => clearTimeout(espera);
  }, [cargar]);

  // Si se llega desde el panel con ?codigo=0012, se abre el detalle de ese pedido.
  const codigo = parametros.get('codigo');
  useEffect(() => {
    if (!codigo) return;
    const hoy = new Date();
    const hace = new Date(hoy.getFullYear() - 1, hoy.getMonth(), hoy.getDate());
    api('/pedidos/buscar' + consulta({ desde: aIso(hace), hasta: aIso(hoy), q: codigo }))
      .then((lista) => setDetalle(lista.find((p) => p.codigo === codigo) ?? null))
      .catch((e) => setError(e.message));
  }, [codigo]);

  function cerrarDetalle() {
    setDetalle(null);
    if (codigo) setParametros({}, { replace: true });
  }

  const columnas = [
    { titulo: 'Pedido', valor: (p) => p.codigo }, { titulo: 'Fecha y hora', valor: (p) => fechaHora(p.fechaHora) },
    { titulo: 'Mesa', valor: (p) => p.mesa }, { titulo: 'Mozo', valor: (p) => p.mozo },
    { titulo: 'Estado', valor: (p) => ETIQUETA_ESTADO_PEDIDO[p.estado] }, { titulo: 'Total (S/)', valor: (p) => Number(p.total).toFixed(2) },
  ];
  const total = (pedidos ?? []).reduce((s, p) => s + Number(p.total), 0);

  return (
    <div>
      <EncabezadoPagina titulo="Pedidos" descripcion="Consulte los pedidos del restaurante, su detalle y su historial de estados.">
        <button type="button" className="boton secundario" onClick={() => exportarCsv(`pedidos-${periodo.desde}-a-${periodo.hasta}`, columnas, pedidos ?? [])} disabled={!pedidos?.length}>
          <Download size={16} /> Exportar a Excel (CSV)
        </button>
        <button type="button" className="boton secundario" onClick={imprimir}><Printer size={16} /> Imprimir / guardar PDF</button>
      </EncabezadoPagina>

      <FiltroPeriodo valor={periodo} onChange={setPeriodo} />
      <div className="barra-filtros no-imprimir">
        <div>
          <label htmlFor="f-estado">Estado</label>
          <select id="f-estado" value={estado} onChange={(e) => setEstado(e.target.value)}>
            <option value="">Todos</option>
            {Object.entries(ETIQUETA_ESTADO_PEDIDO).map(([v, t]) => <option key={v} value={v}>{t}</option>)}
          </select>
        </div>
        <div>
          <label htmlFor="f-mozo">Mozo</label>
          <input id="f-mozo" placeholder="Nombre o usuario" value={mozo} onChange={(e) => setMozo(e.target.value)} />
        </div>
        <CampoBusqueda id="f-texto" etiqueta="Buscar por N.° de pedido o mesa" placeholder="Ej.: 0012 o mesa 5" valor={texto} onChange={setTexto} />
      </div>

      {error && <p className="error" role="alert">{error}</p>}
      {!pedidos ? <Esqueleto alto={220} /> : pedidos.length === 0 ? <Vacio icono={ClipboardList} texto="No hay pedidos con esos filtros." /> : (
        <>
          <p className="nota">{pedidos.length} pedido(s) · {soles(total)} en total{pedidos.length === 500 ? ' · se muestran los 500 más recientes; acote el periodo' : ''}</p>
          <div className="contenedor-tabla">
            <table className="tabla">
              <thead><tr><th>N.°</th><th>Fecha y hora</th><th>Mesa</th><th>Mozo</th><th className="num">Total</th><th>Estado</th><th className="no-imprimir"><span className="solo-lectores">Acción</span></th></tr></thead>
              <tbody>
                {pedidos.map((p) => (
                  <tr key={p.id}>
                    <td><strong>{p.codigo}</strong></td><td>{fechaHora(p.fechaHora)}</td><td>{p.mesa}</td><td>{p.mozo}</td>
                    <td className="num">{soles(p.total)}</td><td><InsigniaPedido estado={p.estado} /></td>
                    <td className="no-imprimir"><button type="button" className="enlace" onClick={() => setDetalle(p)}><Eye size={14} /> Ver detalle</button></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
      {detalle && <DetallePedido pedido={detalle} onCerrar={cerrarDetalle} />}
    </div>
  );
}

function DetallePedido({ pedido, onCerrar }) {
  const [venta, setVenta] = useState(undefined); // undefined = cargando, null = sin cobrar
  const [historial, setHistorial] = useState(null);
  useEffect(() => {
    api(`/ventas/por-pedido/${pedido.codigo}`).then(setVenta).catch(() => setVenta(null));
    api(`/auditoria/pedidos/${pedido.codigo}`).then(setHistorial).catch(() => setHistorial([]));
  }, [pedido.codigo]);
  return (
    <Modal titulo={`Pedido ${pedido.codigo}`} onCerrar={onCerrar}>
      <dl className="detalle-lista">
        <dt>Fecha y hora</dt><dd>{fechaHora(pedido.fechaHora)}</dd>
        <dt>Mesa</dt><dd>{pedido.mesa}</dd>
        <dt>Mozo responsable</dt><dd>{pedido.mozo}</dd>
        <dt>Estado</dt><dd><InsigniaPedido estado={pedido.estado} /></dd>
        <dt>Método de pago</dt><dd>{venta === undefined ? 'Consultando…' : venta ? venta.metodoPago : 'Aún sin cobrar'}</dd>
      </dl>
      <div className="contenedor-tabla">
        <table className="tabla">
          <thead><tr><th>Producto</th><th className="num">Cant.</th><th className="num">P. unit.</th><th className="num">Subtotal</th></tr></thead>
          <tbody>
            {pedido.detalles.map((d, i) => (
              <tr key={i}>
                <td>{d.producto} ({d.presentacion}){d.extraQueso ? ' + extra queso' : ''}{d.paraLlevar ? ' · para llevar' : ''}</td>
                <td className="num">{d.cantidad}</td><td className="num">{soles(d.precioUnitario)}</td><td className="num">{soles(d.subtotal)}</td>
              </tr>
            ))}
          </tbody>
          <tfoot><tr><td colSpan={3} className="num"><strong>Total</strong></td><td className="num"><strong>{soles(pedido.total)}</strong></td></tr></tfoot>
        </table>
      </div>
      <h4 style={{ marginTop: 14 }}>Historial</h4>
      {!historial ? <Esqueleto alto={60} /> : historial.length === 0 ? <p className="nota">Sin movimientos registrados.</p> : (
        <ul className="linea-tiempo">
          {historial.map((h) => (
            <li key={h.id}><strong>{ETIQUETA_HISTORIAL[h.tipo] ?? h.tipo}</strong> — {h.descripcion}<small>{fechaHora(h.fechaHora)} · {h.usuario}</small></li>
          ))}
        </ul>
      )}
    </Modal>
  );
}
