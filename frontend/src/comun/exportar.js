// Exportación: CSV (se abre en Excel) e impresión (guardar como PDF desde el navegador).
// Las celdas que empiezan con = + - @ se neutralizan para evitar la inyección de fórmulas en Excel.
function celda(valor) {
  let texto = valor == null ? '' : String(valor);
  if (/^[=+\-@\t\r]/.test(texto)) texto = "'" + texto;
  return /[",\n]/.test(texto) ? '"' + texto.replace(/"/g, '""') + '"' : texto;
}

export function csvDe(columnas, filas) {
  const cabecera = columnas.map((c) => celda(c.titulo)).join(',');
  const cuerpo = filas.map((f) => columnas.map((c) => celda(c.valor(f))).join(','));
  return '\uFEFF' + [cabecera, ...cuerpo].join('\r\n');
}

export function exportarCsv(nombreArchivo, columnas, filas) {
  const blob = new Blob([csvDe(columnas, filas)], { type: 'text/csv;charset=utf-8' });
  const enlace = document.createElement('a');
  enlace.href = URL.createObjectURL(blob);
  enlace.download = nombreArchivo.endsWith('.csv') ? nombreArchivo : nombreArchivo + '.csv';
  document.body.appendChild(enlace);
  enlace.click();
  enlace.remove();
  setTimeout(() => URL.revokeObjectURL(enlace.href), 1000);
}

export function imprimir() {
  window.print();
}
