import { render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import Reportes from './Reportes.jsx';

const VENTAS = {
  cantidadVentas: 3, totalVendido: 120, ticketPromedio: 40,
  porCategoria: [{ nombre: 'Pizzas clásicas', monto: 100 }, { nombre: 'Alitas', monto: 20 }],
  masVendidos: [{ nombre: 'Hawaiana', cantidad: 4 }],
  porMetodoPago: [{ nombre: 'Efectivo', monto: 120 }],
  porCajero: [{ nombre: 'Ana Pérez', monto: 120 }],
};
const INVENTARIO = { insumos: [{ insumo: 'Pollo', unidad: 'kg', entradas: 10, salidas: 4, stockActual: 36 }] };
const SERIE = [{ periodo: '2026-11-18', ventas: 3, monto: 120 }];
const PEDIDOS = { total: 3, porEstado: [{ nombre: 'Completado', cantidad: 3 }], porMozo: [] };

function respuesta(estado, cuerpo) {
  return Promise.resolve({ ok: estado >= 200 && estado < 300, status: estado, json: () => Promise.resolve(cuerpo) });
}

function respuestaPorRuta(url) {
  if (url.startsWith('/api/reportes/ventas')) return respuesta(200, VENTAS);
  if (url.startsWith('/api/reportes/inventario')) return respuesta(200, INVENTARIO);
  if (url.startsWith('/api/reportes/serie')) return respuesta(200, SERIE);
  if (url.startsWith('/api/reportes/pedidos')) return respuesta(200, PEDIDOS);
  return respuesta(404, {});
}

describe('Reportes (HU-015)', () => {
  afterEach(() => vi.unstubAllGlobals());

  it('muestra las cifras de venta, categorías e inventario del periodo', async () => {
    vi.stubGlobal('fetch', vi.fn(respuestaPorRuta));
    render(<Reportes />);

    expect(await screen.findByTestId('total-vendido')).toHaveTextContent('S/ 120.00');
    expect(screen.getByTestId('cantidad-ventas')).toHaveTextContent('3');
    expect(screen.getAllByText('Pizzas clásicas').length).toBeGreaterThan(0);
    expect(screen.getByText('Pollo')).toBeInTheDocument();
    expect(screen.getByText('36.00')).toBeInTheDocument();
    expect(await screen.findByText('Ana Pérez')).toBeInTheDocument();
  });

  it('muestra el mensaje del servidor si el rango de fechas no es válido', async () => {
    vi.stubGlobal('fetch', vi.fn(() => respuesta(400, { mensaje: 'La fecha final no puede ser anterior a la fecha inicial.' })));
    render(<Reportes />);

    expect(await screen.findByRole('alert')).toHaveTextContent('La fecha final no puede ser anterior');
  });
});
