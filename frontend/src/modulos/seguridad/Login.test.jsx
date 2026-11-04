import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { AuthProvider } from './auth.jsx';
import Login from './Login.jsx';

function mostrar() {
  return render(
    <MemoryRouter>
      <AuthProvider>
        <Login />
      </AuthProvider>
    </MemoryRouter>
  );
}

function respuesta(estado, cuerpo) {
  return Promise.resolve({ ok: estado >= 200 && estado < 300, status: estado, json: () => Promise.resolve(cuerpo) });
}

describe('Pantalla de inicio de sesión (HU-001)', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    localStorage.clear();
  });

  it('muestra los campos de usuario y contraseña y no ofrece registrarse', () => {
    mostrar();
    expect(screen.getByLabelText('Usuario')).toBeInTheDocument();
    expect(screen.getByLabelText('Contraseña')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Ingresar' })).toBeInTheDocument();
    expect(screen.queryByText(/registrarse/i)).not.toBeInTheDocument();
  });

  it('con datos incorrectos muestra un único mensaje de error', async () => {
    vi.stubGlobal('fetch', vi.fn(() => respuesta(401, { mensaje: 'Usuario o contraseña incorrectos' })));
    mostrar();

    await userEvent.type(screen.getByLabelText('Usuario'), 'mozo01');
    await userEvent.type(screen.getByLabelText('Contraseña'), 'mala');
    await userEvent.click(screen.getByRole('button', { name: 'Ingresar' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Usuario o contraseña incorrectos');
    expect(localStorage.getItem('token')).toBeNull();
  });

  it('con datos correctos guarda la sesión', async () => {
    const usuario = { id: 1, nombreUsuario: 'gerente', nombreCompleto: 'Gerente', rol: 'GERENTE', permisos: ['USUARIOS_GESTIONAR'] };
    vi.stubGlobal('fetch', vi.fn(() => respuesta(200, { token: 'abc123', usuario })));
    mostrar();

    await userEvent.type(screen.getByLabelText('Usuario'), 'gerente');
    await userEvent.type(screen.getByLabelText('Contraseña'), 'Gerente123*');
    await userEvent.click(screen.getByRole('button', { name: 'Ingresar' }));

    await vi.waitFor(() => expect(localStorage.getItem('token')).toBe('abc123'));
  });
});
