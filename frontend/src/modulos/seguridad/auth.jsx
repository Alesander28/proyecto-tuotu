import { createContext, useContext, useState } from 'react';
import { api } from '../../comun/api.js';

const AuthContext = createContext(null);

function leerUsuario() {
  try {
    const expira = Number(sessionStorage.getItem('tokenExpiraEn') || 0);
    if (!sessionStorage.getItem('token') || !expira || Date.now() >= expira) {
      sessionStorage.removeItem('token');
      sessionStorage.removeItem('usuario');
      sessionStorage.removeItem('tokenExpiraEn');
      return null;
    }
    return JSON.parse(sessionStorage.getItem('usuario'));
  } catch {
    sessionStorage.removeItem('token');
    sessionStorage.removeItem('usuario');
    sessionStorage.removeItem('tokenExpiraEn');
    return null;
  }
}

export function AuthProvider({ children }) {
  const [usuario, setUsuario] = useState(leerUsuario());

  // HU-001: iniciar sesión
  async function iniciarSesion(nombreUsuario, contrasena) {
    const datos = await api('/auth/login', { metodo: 'POST', cuerpo: { nombreUsuario, contrasena } });
    sessionStorage.setItem('token', datos.token);
    sessionStorage.setItem('usuario', JSON.stringify(datos.usuario));
    sessionStorage.setItem('tokenExpiraEn', String(datos.expiraEn ? Number(datos.expiraEn) : Date.now() + 8 * 60 * 60 * 1000));
    setUsuario(datos.usuario);
  }

  // HU-006: cerrar sesión (el servidor invalida el token)
  async function cerrarSesion() {
    try {
      await api('/auth/logout', { metodo: 'POST' });
    } catch {
      // aunque falle la llamada, se limpia la sesión local
    }
    sessionStorage.removeItem('token');
    sessionStorage.removeItem('usuario');
    sessionStorage.removeItem('tokenExpiraEn');
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
