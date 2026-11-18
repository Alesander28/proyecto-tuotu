import { useEffect, useRef } from 'react';
import { Inbox, Minus, Search, TrendingDown, TrendingUp, X } from 'lucide-react';
import { aIso, rangoDe } from './periodos.js';
import { ETIQUETA_ESTADO_PEDIDO } from './formato.js';

// Componentes reutilizables del sistema visual de Tú o Tú.

export function Insignia({ tono = 'neutro', children }) {
  return <span className={`insignia ${tono}`}>{children}</span>;
}

const TONO_PEDIDO = { PENDIENTE: 'amarillo', EN_PREPARACION: 'azul', ATENDIDO: 'verde' };
export function InsigniaPedido({ estado }) {
  return <Insignia tono={TONO_PEDIDO[estado] ?? 'neutro'}>{ETIQUETA_ESTADO_PEDIDO[estado] ?? estado}</Insignia>;
}

const TONO_STOCK = { AGOTADO: 'rojo', BAJO: 'naranja', PROXIMO: 'amarillo', NORMAL: 'verde' };
const ETIQUETA_STOCK = { AGOTADO: 'Agotado', BAJO: 'Stock bajo', PROXIMO: 'Próximo a agotarse', NORMAL: 'Normal' };
export function InsigniaStock({ estado }) {
  return <Insignia tono={TONO_STOCK[estado] ?? 'neutro'}>{ETIQUETA_STOCK[estado] ?? estado}</Insignia>;
}

// Compara un valor con el del periodo anterior. Si no hay con qué comparar, lo dice en lugar de inventar un porcentaje.
export function Tendencia({ actual, anterior, etiqueta = 'vs. periodo anterior' }) {
  const a = Number(actual ?? 0);
  const b = Number(anterior ?? 0);
  if (!b) return <span className="tendencia igual"><Minus size={14} /> Sin datos para comparar</span>;
  const variacion = ((a - b) / b) * 100;
  const clase = variacion > 0.05 ? 'sube' : variacion < -0.05 ? 'baja' : 'igual';
  const Icono = clase === 'sube' ? TrendingUp : clase === 'baja' ? TrendingDown : Minus;
  return (
    <span className={`tendencia ${clase}`} title={etiqueta}>
      <Icono size={14} /> {variacion > 0 ? '+' : ''}{variacion.toFixed(0)}% {etiqueta}
    </span>
  );
}

export function KpiCard({ icono: Icono, titulo, valor, nota, tono = 'naranja', children, cargando }) {
  return (
    <div className={`tarjeta kpi ${tono}`}>
      <div className="kpi-icono" aria-hidden="true"><Icono size={24} /></div>
      <div>
        <div className="kpi-titulo">{titulo}</div>
        {cargando ? <div className="esqueleto" style={{ width: 90, height: 28, marginTop: 4 }} /> : <div className="kpi-valor">{valor}</div>}
        {children}
        {nota && <div className="kpi-nota">{nota}</div>}
      </div>
    </div>
  );
}

export function Esqueleto({ alto = 120 }) {
  return <div className="esqueleto" style={{ height: alto }} role="status" aria-label="Cargando" />;
}

export function Vacio({ icono: Icono = Inbox, texto = 'No hay datos para mostrar.' }) {
  return (
    <div className="vacio">
      <Icono size={34} aria-hidden="true" />
      <div>{texto}</div>
    </div>
  );
}

export function EncabezadoPagina({ titulo, descripcion, children }) {
  return (
    <div className="fila-titulo">
      <div>
        <h2>{titulo}</h2>
        {descripcion && <p>{descripcion}</p>}
      </div>
      {children && <div className="acciones-pagina no-imprimir">{children}</div>}
    </div>
  );
}

// Ventana emergente accesible: se cierra con Escape o al pulsar fuera.
export function Modal({ titulo, onCerrar, children, estrecho = false }) {
  const ref = useRef(null);
  useEffect(() => {
    const alTeclear = (e) => e.key === 'Escape' && onCerrar();
    document.addEventListener('keydown', alTeclear);
    ref.current?.focus();
    return () => document.removeEventListener('keydown', alTeclear);
  }, [onCerrar]);
  return (
    <div className="fondo-modal" onMouseDown={(e) => e.target === e.currentTarget && onCerrar()}>
      <div className={estrecho ? 'modal estrecho' : 'modal'} role="dialog" aria-modal="true" aria-label={titulo} tabIndex={-1} ref={ref}>
        <div className="modal-cabecera">
          <h3>{titulo}</h3>
          <button type="button" className="boton-icono" onClick={onCerrar} aria-label="Cerrar"><X size={18} /></button>
        </div>
        {children}
      </div>
    </div>
  );
}

// Confirmación antes de una operación crítica.
export function Confirmar({ titulo, mensaje, textoBoton = 'Confirmar', peligro = false, enviando = false, onConfirmar, onCancelar }) {
  return (
    <Modal titulo={titulo} onCerrar={onCancelar} estrecho>
      <p>{mensaje}</p>
      <div className="acciones">
        <button type="button" className={peligro ? 'boton peligro' : 'boton'} onClick={onConfirmar} disabled={enviando}>
          {enviando ? 'Procesando…' : textoBoton}
        </button>
        <button type="button" className="boton secundario" onClick={onCancelar}>Cancelar</button>
      </div>
    </Modal>
  );
}

// Filtro de periodo: Hoy, Esta semana, Este mes o fechas a elección.
export function FiltroPeriodo({ valor, onChange }) {
  const { tipo, desde, hasta } = valor;
  const elegir = (t) => onChange({ tipo: t, ...(t === 'personalizado' ? { desde, hasta } : rangoDe(t)) });
  return (
    <div className="barra-filtros no-imprimir" role="group" aria-label="Periodo">
      <div className="conmutador">
        {[['hoy', 'Hoy'], ['semana', 'Esta semana'], ['mes', 'Este mes'], ['personalizado', 'Fechas']].map(([t, texto]) => (
          <button key={t} type="button" aria-pressed={tipo === t} onClick={() => elegir(t)}>{texto}</button>
        ))}
      </div>
      {tipo === 'personalizado' && (
        <>
          <div>
            <label htmlFor="fp-desde">Desde</label>
            <input id="fp-desde" type="date" value={desde} max={hasta} onChange={(e) => onChange({ tipo, desde: e.target.value, hasta })} />
          </div>
          <div>
            <label htmlFor="fp-hasta">Hasta</label>
            <input id="fp-hasta" type="date" value={hasta} min={desde} max={aIso(new Date())} onChange={(e) => onChange({ tipo, desde, hasta: e.target.value })} />
          </div>
        </>
      )}
    </div>
  );
}

export function CampoBusqueda({ id, valor, onChange, placeholder = 'Buscar…', etiqueta }) {
  return (
    <div className="campo-ancho">
      <label htmlFor={id}>{etiqueta ?? placeholder}</label>
      <div className="campo-icono">
        <Search size={16} aria-hidden="true" />
        <input id={id} type="search" placeholder={placeholder} value={valor} onChange={(e) => onChange(e.target.value)} />
      </div>
    </div>
  );
}

export function periodoInicial(tipo = 'hoy') {
  return { tipo, ...rangoDe(tipo) };
}
