// Rangos de fechas para los filtros (hoy, esta semana, este mes) y comparación con el periodo anterior.
const dos = (n) => String(n).padStart(2, '0');

export function aIso(fecha) {
  return `${fecha.getFullYear()}-${dos(fecha.getMonth() + 1)}-${dos(fecha.getDate())}`;
}

export function desdeIso(texto) {
  const [a, m, d] = texto.split('-').map(Number);
  return new Date(a, m - 1, d);
}

export function rangoDe(tipo, hoy = new Date()) {
  const base = new Date(hoy.getFullYear(), hoy.getMonth(), hoy.getDate());
  if (tipo === 'semana') {
    const lunes = new Date(base);
    lunes.setDate(base.getDate() - ((base.getDay() + 6) % 7));
    return { desde: aIso(lunes), hasta: aIso(base) };
  }
  if (tipo === 'mes') return { desde: aIso(new Date(base.getFullYear(), base.getMonth(), 1)), hasta: aIso(base) };
  return { desde: aIso(base), hasta: aIso(base) };
}

// Mismo número de días inmediatamente antes del rango dado.
export function periodoAnterior({ desde, hasta }) {
  const a = desdeIso(desde);
  const b = desdeIso(hasta);
  const dias = Math.round((b - a) / 86400000) + 1;
  const finAnterior = new Date(a);
  finAnterior.setDate(a.getDate() - 1);
  const inicioAnterior = new Date(finAnterior);
  inicioAnterior.setDate(finAnterior.getDate() - (dias - 1));
  return { desde: aIso(inicioAnterior), hasta: aIso(finAnterior) };
}

export function consulta(parametros) {
  const p = Object.entries(parametros).filter(([, v]) => v !== '' && v != null);
  return p.length ? '?' + p.map(([k, v]) => `${k}=${encodeURIComponent(v)}`).join('&') : '';
}
