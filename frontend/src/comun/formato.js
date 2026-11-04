// Formato de dinero: S/ 22.00
export function soles(valor) {
  return 'S/ ' + Number(valor).toFixed(2);
}
