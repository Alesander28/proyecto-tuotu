import { createContext, useContext, useState } from 'react';
import { api } from '../../comun/api.js';

const AuthContext = createContext(null);

function leerUsuario() {
  try {
    return JSON.parse(localStorage.getItem('usuario'));
  } catch {
    return null;
  }
}

export function AuthProvider({ children }) {
  const [usuario, setUsuario] = useState(leerUsuario());

  // HU-001: iniciar sesión
  async function iniciarSesion(nombreUsuario, contrasena) {
    const datos = await api('/auth/login', { metodo: 'POST', cuerpo: { nombreUsuario, contrasena } });
    localStorage.setItem('token', datos.token);
    localStorage.setItem('usuario', JSON.stringify(datos.usuario));
    setUsuario(datos.usuario);
  }

  // HU-006: cerrar sesión (el servidor invalida el token)
  async function cerrarSesion() {
    try {
      await api('/auth/logout', { metodo: 'POST' });
    } catch {
      // aunque falle la llamada, se limpia la sesión local
    }
    localStorage.removeItem('token');
    localStorage.removeItem('usuario');
    setUsuario(null);
  }

  const tienePermiso = (codigo) => !!usuario && usuario.permisos.includes(codigo);

  return (
    <AuthContext.Provider value={{ usuario, iniciarSesion, cerrarSesion, tienePermiso }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
