import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import Inventario from './Inventario.jsx';
import Productos from './Productos.jsx';

const INSUMOS = [{ id: 1, nombre: 'Pollo', unidadMedida: 'kg', stockActual: 30 }];

function respuesta(estado, cuerpo) {
  return Promise.resolve({ ok: estado >= 200 && estado < 300, status: estado, json: () => Promise.resolve(cuerpo) });
}

describe('Inventario (HU-011, HU-012 y HU-013)', () => {
  afterEach(() => vi.unstubAllGlobals());

  it('muestra el stock actual de cada insumo', async () => {
    vi.stubGlobal('fetch', vi.fn(() => respuesta(200, INSUMOS)));
    render(<Inventario />);

    expect(await screen.findByText('Pollo')).toBeInTheDocument();
    expect(screen.getByText('30.00')).toBeInTheDocument();
  });

  it('muestra el mensaje del servidor si la salida supera el stock', async () => {
    vi.stubGlobal('fetch', vi.fn((url, opciones) => {
      if (opciones?.method === 'POST') return respuesta(409, { mensaje: 'No hay stock suficiente de «Pollo». Stock actual: 30.00 kg.' });
      return respuesta(200, INSUMOS);
    }));
    render(<Inventario />);

    await userEvent.click(await screen.findByRole('button', { name: 'Registrar salida' }));
    await userEvent.type(screen.getByLabelText(/Cantidad/), '50');
    await userEvent.type(screen.getByLabelText(/Motivo/), 'Consumo');
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('No hay stock suficiente');
  });
});

describe('Productos (HU-010)', () => {
  afterEach(() => vi.unstubAllGlobals());

  it('lista los productos con sus precios y permite desactivarlos', async () => {
    const producto = { id: 5, nombre: 'Hawaiana', activo: true, categoriaId: 1, categoria: 'Pizzas clásicas',
      presentaciones: [{ id: 50, nombre: 'Personal', precio: 22 }] };
    vi.stubGlobal('fetch', vi.fn((url) => respuesta(200, url.endsWith('/categorias') ? [{ id: 1, nombre: 'Pizzas clásicas' }] : [producto])));
    render(<Productos />);

    expect(await screen.findByText('Hawaiana')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Cambiar precio de Hawaiana Personal/ })).toHaveTextContent('S/ 22.00');
    expect(screen.getByRole('button', { name: 'Desactivar' })).toBeInTheDocument();
  });
});
