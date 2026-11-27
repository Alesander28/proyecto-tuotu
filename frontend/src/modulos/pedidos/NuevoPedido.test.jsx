import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import NuevoPedido from './NuevoPedido.jsx';


const CARTA = {
  categorias: [
    {
      id: 1, nombre: 'Pizzas', recargoParaLlevar: 2.0,
      productos: [{ id: 10, nombre: 'Hawaiana', presentaciones: [
        { id: 100, nombre: 'Personal', precio: 22.0, recargoExtraQueso: 3.0 },
        { id: 101, nombre: 'Familiar', precio: 42.0, recargoExtraQueso: 5.0 },
      ] }],
    },
    {
      id: 2, nombre: 'Jugos', recargoParaLlevar: 0,
      productos: [{ id: 20, nombre: 'Jugo de papaya', presentaciones: [
        { id: 200, nombre: 'Simple', precio: 5.0, recargoExtraQueso: null },
      ] }],
    },
  ],
};
const MESAS = [
  { id: 1, numero: '01', activa: true, estado: 'LIBRE' },
  { id: 2, numero: '02', activa: true, estado: 'OCUPADA' },
  { id: 3, numero: '03', activa: false, estado: 'LIBRE' },
  { id: 4, numero: '04', activa: true, estado: 'LIBRE' },
];

function respuesta(estado, cuerpo) {
  return Promise.resolve({ ok: estado >= 200 && estado < 300, status: estado, json: () => Promise.resolve(cuerpo) });
}
function instalarFetch({ mesas = MESAS, pedido } = {}) {
  const fetchSimulado = vi.fn((url) => {
    if (url === '/api/carta') return respuesta(200, CARTA);
    if (url === '/api/mesas') return respuesta(200, mesas);
    if (url === '/api/pedidos') return respuesta(201, pedido || { codigo: '0012', mesa: '01', total: 22.0, estado: 'PENDIENTE' });
    return respuesta(404, {});
  });
  vi.stubGlobal('fetch', fetchSimulado);
  return fetchSimulado;
}

describe('Nuevo pedido (HU-007)', () => {
  afterEach(() => vi.unstubAllGlobals());

  it('muestra únicamente las mesas activas y libres registradas por el gerente', async () => {
    instalarFetch();
    render(<MemoryRouter><NuevoPedido /></MemoryRouter>);
    const selector = await screen.findByLabelText('Mesa');
    expect(await screen.findByRole('option', { name: 'Mesa 01 — Disponible' })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'Mesa 04 — Disponible' })).toBeInTheDocument();
    expect(screen.queryByRole('option', { name: /Mesa 02/ })).not.toBeInTheDocument();
    expect(screen.queryByRole('option', { name: /Mesa 03/ })).not.toBeInTheDocument();
    expect(selector.tagName).toBe('SELECT');
  });

  it('calcula el total con los cargos de extra queso y para llevar', async () => {
    instalarFetch();
    render(<MemoryRouter><NuevoPedido /></MemoryRouter>);
    await userEvent.click(await screen.findByRole('button', { name: 'Agregar Hawaiana Personal' }));
    expect(screen.getByTestId('total')).toHaveTextContent('S/ 22.00');
    await userEvent.click(screen.getByRole('button', { name: 'Agregar Hawaiana Familiar' }));
    expect(screen.getByTestId('total')).toHaveTextContent('S/ 64.00');
    await userEvent.click(screen.getAllByLabelText(/Extra queso/)[0]);
    expect(screen.getByTestId('total')).toHaveTextContent('S/ 67.00');
    await userEvent.click(screen.getAllByLabelText(/Para llevar/)[0]);
    expect(screen.getByTestId('total')).toHaveTextContent('S/ 69.00');
  });

  it('el extra queso depende del tamaño y solo aparece en los productos que lo admiten', async () => {
    instalarFetch();
    render(<MemoryRouter><NuevoPedido /></MemoryRouter>);
    await userEvent.click(await screen.findByRole('button', { name: 'Agregar Hawaiana Familiar' }));
    await userEvent.click(screen.getByLabelText(/Extra queso/));
    expect(screen.getByTestId('total')).toHaveTextContent('S/ 47.00');
    await userEvent.click(screen.getByRole('tab', { name: 'Jugos' }));
    await userEvent.click(screen.getByRole('button', { name: 'Agregar Jugo de papaya Simple' }));
    expect(screen.getAllByLabelText(/Extra queso/)).toHaveLength(1);
    await userEvent.click(screen.getAllByLabelText(/Para llevar/)[1]);
    expect(screen.getByTestId('total')).toHaveTextContent('S/ 52.00');
  });

  it('no permite enviar un pedido sin seleccionar mesa', async () => {
    instalarFetch();
    render(<MemoryRouter><NuevoPedido /></MemoryRouter>);
    await userEvent.click(await screen.findByRole('button', { name: 'Agregar Hawaiana Personal' }));
    expect(screen.getByRole('button', { name: 'Enviar pedido' })).toBeDisabled();
  });

  it('envía el pedido con la mesa seleccionada y muestra su código', async () => {
    const fetchSimulado = instalarFetch();
    render(<MemoryRouter><NuevoPedido /></MemoryRouter>);
    await screen.findByRole('option', { name: 'Mesa 01 — Disponible' });
    await userEvent.selectOptions(screen.getByLabelText('Mesa'), '01');
    await userEvent.click(screen.getByRole('button', { name: 'Agregar Hawaiana Personal' }));
    await userEvent.click(screen.getByRole('button', { name: 'Enviar pedido' }));
    expect(await screen.findByRole('status')).toHaveTextContent('Pedido 0012 registrado');
    const llamada = fetchSimulado.mock.calls.find(([url]) => url === '/api/pedidos');
    expect(JSON.parse(llamada[1].body).mesa).toBe('01');
  });

  it('informa cuando el gerente todavía no registró mesas disponibles', async () => {
    instalarFetch({ mesas: [] });
    render(<MemoryRouter><NuevoPedido /></MemoryRouter>);
    expect(await screen.findByText(/No hay mesas disponibles/)).toBeInTheDocument();
  });
});
