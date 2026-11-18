// Formatos de dinero, fechas y textos.
export function soles(valor) {
  return 'S/ ' + Number(valor ?? 0).toFixed(2);
}

export function capitalizar(texto) {
  const t = String(texto ?? '').toLowerCase();
  return t.charAt(0).toUpperCase() + t.slice(1);
}

export function fechaLarga(fecha = new Date()) {
  return capitalizar(fecha.toLocaleDateString('es-PE', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' }));
}

export function fechaHora(texto) {
  if (!texto) return '—';
  return new Date(texto).toLocaleString('es-PE', { dateStyle: 'short', timeStyle: 'short' });
}

export function hora(texto) {
  if (!texto) return '—';
  return new Date(texto).toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' });
}

export function minutosATexto(minutos) {
  if (minutos == null) return '—';
  if (minutos < 60) return `${minutos} min`;
  return `${Math.floor(minutos / 60)} h ${minutos % 60} min`;
}

export const ETIQUETA_ESTADO_PEDIDO = { PENDIENTE: 'Pendiente', EN_PREPARACION: 'En preparación', ATENDIDO: 'Atendido' };
