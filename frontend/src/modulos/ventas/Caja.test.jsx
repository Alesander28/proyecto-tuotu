import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import Caja from './Caja.jsx';

const PEDIDO = {
  id: 12, codigo: '0012', mesa: '5', mozo: 'Mozo Uno', total: 50, estado: 'ATENDIDO',
  detalles: [{ producto: 'Hawaiana', presentacion: 'Personal', cantidad: 2, subtotal: 50, extraQueso: true, paraLlevar: false }],
};

function respuesta(estado, cuerpo) {
  return Promise.resolve({ ok: estado >= 200 && estado < 300, status: estado, json: () => Promise.resolve(cuerpo) });
}

function servidor(alRegistrar) {
  return vi.fn((url, opciones) => {
    if (opciones?.method === 'POST') return alRegistrar();
    if (url === '/api/ventas/pendientes') return respuesta(200, [PEDIDO]);
    if (url === '/api/ventas/metodos-pago') return respuesta(200, ['Efectivo', 'Yape']);
    return respuesta(200, []);
  });
}

describe('Caja (HU-009 y HU-014)', () => {
  afterEach(() => vi.unstubAllGlobals());

  it('muestra los pedidos atendidos pendientes de cobro y su detalle', async () => {
    vi.stubGlobal('fetch', servidor(() => respuesta(201, {})));
    render(<Caja />);

    await userEvent.click(await screen.findByRole('button', { name: 'Seleccionar pedido 0012' }));

    expect(screen.getByTestId('total-venta')).toHaveTextContent('S/ 50.00');
    expect(screen.getByText(/2 × Hawaiana \(Personal\)/)).toBeInTheDocument();
  });

  it('registra la venta y muestra el número de la boleta', async () => {
    vi.stubGlobal('fetch', servidor(() => respuesta(201, {
      id: 1, pedidoCodigo: '0012', total: 50, comprobanteId: 3, comprobanteNumero: 'B001-00000003',
    })));
    render(<Caja />);

    await userEvent.click(await screen.findByRole('button', { name: 'Seleccionar pedido 0012' }));
    await userEvent.selectOptions(screen.getByLabelText('Método de pago'), 'Yape');
    await userEvent.click(screen.getByRole('button', { name: /Registrar venta/ }));

    expect(await screen.findByRole('status')).toHaveTextContent('Boleta B001-00000003');
  });

  it('muestra el mensaje del servidor si el pedido ya fue cobrado', async () => {
    vi.stubGlobal('fetch', servidor(() => respuesta(409, { mensaje: 'El pedido 0012 ya fue cobrado.' })));
    render(<Caja />);

    await userEvent.click(await screen.findByRole('button', { name: 'Seleccionar pedido 0012' }));
    await userEvent.click(screen.getByRole('button', { name: /Registrar venta/ }));

    expect(await screen.findByRole('alert')).toHaveTextContent('ya fue cobrado');
  });
});
