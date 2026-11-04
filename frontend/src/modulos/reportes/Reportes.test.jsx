import { render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import Reportes from './Reportes.jsx';

const VENTAS = {
  cantidadVentas: 3, totalVendido: 120,
  porCategoria: [{ nombre: 'Pizzas clásicas', monto: 100 }, { nombre: 'Alitas', monto: 20 }],
  masVendidos: [{ nombre: 'Hawaiana', cantidad: 4 }],
  porMetodoPago: [{ nombre: 'Efectivo', monto: 120 }],
};
const INVENTARIO = { insumos: [{ insumo: 'Pollo', unidad: 'kg', entradas: 10, salidas: 4, stockActual: 36 }] };

function respuesta(estado, cuerpo) {
  return Promise.resolve({ ok: estado >= 200 && estado < 300, status: estado, json: () => Promise.resolve(cuerpo) });
}

describe('Reportes (HU-015)', () => {
  afterEach(() => vi.unstubAllGlobals());

  it('muestra los totales de ventas y el inventario del periodo', async () => {
    vi.stubGlobal('fetch', vi.fn((url) => respuesta(200, url.startsWith('/api/reportes/ventas') ? VENTAS : INVENTARIO)));
    render(<Reportes />);

    expect(await screen.findByTestId('total-vendido')).toHaveTextContent('S/ 120.00');
    expect(screen.getByTestId('cantidad-ventas')).toHaveTextContent('3');
    expect(screen.getAllByText('Pizzas clásicas').length).toBeGreaterThan(0);
    expect(screen.getByText('Pollo')).toBeInTheDocument();
    expect(screen.getByText('36.00')).toBeInTheDocument();
  });

  it('muestra el mensaje del servidor si el rango de fechas no es válido', async () => {
    vi.stubGlobal('fetch', vi.fn(() => respuesta(400, { mensaje: 'La fecha final no puede ser anterior a la fecha inicial.' })));
    render(<Reportes />);

    expect(await screen.findByRole('alert')).toHaveTextContent('La fecha final no puede ser anterior');
  });
});
