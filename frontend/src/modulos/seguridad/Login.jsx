import { useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { useAuth } from './auth.jsx';

export default function Login() {
  const { usuario, iniciarSesion } = useAuth();
  const navegar = useNavigate();
  const [nombreUsuario, setNombreUsuario] = useState('');
  const [contrasena, setContrasena] = useState('');
  const [error, setError] = useState('');
  const [enviando, setEnviando] = useState(false);

  if (usuario) return <Navigate to="/" replace />;

  async function enviar(evento) {
    evento.preventDefault();
    setError('');
    setEnviando(true);
    try {
      await iniciarSesion(nombreUsuario.trim(), contrasena);
      navegar('/');
    } catch (e) {
      setError(e.message);
    } finally {
      setEnviando(false);
    }
  }

  return (
    <main className="login-fondo">
      <form className="tarjeta login" onSubmit={enviar} aria-labelledby="titulo-login">
        <div className="logo" aria-hidden="true">T o T</div>
        <h1 id="titulo-login">Sistema de gestión</h1>

        <label htmlFor="usuario">Usuario</label>
        <input id="usuario" value={nombreUsuario} onChange={(e) => setNombreUsuario(e.target.value)}
               autoComplete="username" required />

        <label htmlFor="contrasena">Contraseña</label>
        <input id="contrasena" type="password" value={contrasena}
               onChange={(e) => setContrasena(e.target.value)} autoComplete="current-password" required />

        {error && <p className="error" role="alert">{error}</p>}

        <button className="boton" type="submit" disabled={enviando}>
          {enviando ? 'Ingresando…' : 'Ingresar'}
        </button>
      </form>
    </main>
  );
}
