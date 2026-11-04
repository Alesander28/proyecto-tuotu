import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import NuevoPedido from './NuevoPedido.jsx';

const CARTA = {
  recargoExtraQueso: 3.0,
  recargoParaLlevar: 2.0,
  categorias: [{
    id: 1, nombre: 'Pizzas clásicas',
    productos: [{ id: 10, nombre: 'Hawaiana', presentaciones: [
      { id: 100, nombre: 'Personal', precio: 22.0 },
      { id: 101, nombre: 'Familiar', precio: 42.0 },
    ] }],
  }],
};

function respuesta(estado, cuerpo) {
  return Promise.resolve({ ok: estado >= 200 && estado < 300, status: estado, json: () => Promise.resolve(cuerpo) });
}

describe('Nuevo pedido (HU-007)', () => {
  afterEach(() => vi.unstubAllGlobals());

  it('calcula el total con los cargos de extra queso y para llevar', async () => {
    vi.stubGlobal('fetch', vi.fn(() => respuesta(200, CARTA)));
    render(<NuevoPedido />);

    await userEvent.click(await screen.findByRole('button', { name: 'Agregar Hawaiana Personal' }));
    expect(screen.getByTestId('total')).toHaveTextContent('S/ 22.00');

    await userEvent.click(screen.getByRole('button', { name: 'Agregar Hawaiana Familiar' }));
    expect(screen.getByTestId('total')).toHaveTextContent('S/ 64.00');

    await userEvent.click(screen.getAllByLabelText(/Extra queso/)[0]);
    expect(screen.getByTestId('total')).toHaveTextContent('S/ 67.00');

    await userEvent.click(screen.getAllByLabelText(/Para llevar/)[0]);
    expect(screen.getByTestId('total')).toHaveTextContent('S/ 69.00');
  });

  it('no envía un pedido sin mesa', async () => {
    const fetchSimulado = vi.fn(() => respuesta(200, CARTA));
    vi.stubGlobal('fetch', fetchSimulado);
    render(<NuevoPedido />);

    await userEvent.click(await screen.findByRole('button', { name: 'Agregar Hawaiana Personal' }));
    await userEvent.click(screen.getByRole('button', { name: 'Enviar pedido' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Indique la mesa.');
    expect(fetchSimulado).toHaveBeenCalledTimes(1); // solo la carga de la carta
  });

  it('envía el pedido y muestra su código', async () => {
    const fetchSimulado = vi.fn((url) => {
      if (url === '/api/carta') return respuesta(200, CARTA);
      return respuesta(201, { codigo: '0012', mesa: '5', total: 22.0, estado: 'PENDIENTE' });
    });
    vi.stubGlobal('fetch', fetchSimulado);
    render(<NuevoPedido />);

    await userEvent.type(await screen.findByLabelText('Mesa'), '5');
    await userEvent.click(screen.getByRole('button', { name: 'Agregar Hawaiana Personal' }));
    await userEvent.click(screen.getByRole('button', { name: 'Enviar pedido' }));

    expect(await screen.findByRole('status')).toHaveTextContent('Pedido 0012 registrado');
  });
});
