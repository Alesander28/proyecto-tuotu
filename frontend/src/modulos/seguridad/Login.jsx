import { useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { Eye, EyeOff, LockKeyhole, LogIn, UserRound } from 'lucide-react';
import { useAuth } from './auth.jsx';
import './login.css';

export default function Login() {
  const { usuario, iniciarSesion } = useAuth();
  const navegar = useNavigate();
  const [nombreUsuario, setNombreUsuario] = useState('');
  const [contrasena, setContrasena] = useState('');
  const [error, setError] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [mostrarContrasena, setMostrarContrasena] = useState(false);

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
    <main className="login-nuevo">
      {/* La imagen de fondo ya incluye el logo y el eslogan; no se superponen elementos. */}
      <section className="login-marca" aria-label="Tú o Tú: pizzas, tacos y parrillas. Sabor que nos une">
        <div className="login-marca-fondo" aria-hidden="true" />
      </section>

      <section className="login-panel-lado" aria-label="Formulario de inicio de sesión">
        <div className="login-panel">
          <div className="login-decor login-decor-superior" aria-hidden="true">✦</div>
          <img className="login-logo-form" src="/logo-tuotu.png" alt="Logo Tú o Tú" />
          <h1 id="titulo-login">Bienvenido</h1>
          <p className="login-subtitulo">Inicia sesión para continuar</p>
          <div className="login-separador"><span /></div>

          <form onSubmit={enviar} aria-labelledby="titulo-login">
            <label className="login-campo" htmlFor="usuario">
              <UserRound aria-hidden="true" />
              <input id="usuario" value={nombreUsuario} onChange={(e) => setNombreUsuario(e.target.value)}
                autoComplete="username" placeholder="Usuario" required autoFocus />
            </label>

            <label className="login-campo" htmlFor="contrasena">
              <LockKeyhole aria-hidden="true" />
              <input id="contrasena" type={mostrarContrasena ? 'text' : 'password'} value={contrasena}
                onChange={(e) => setContrasena(e.target.value)} autoComplete="current-password"
                placeholder="Contraseña" required />
              <button className="login-ver-clave" type="button" onClick={() => setMostrarContrasena((v) => !v)}
                aria-label={mostrarContrasena ? 'Ocultar contraseña' : 'Mostrar contraseña'}>
                {mostrarContrasena ? <EyeOff aria-hidden="true" /> : <Eye aria-hidden="true" />}
              </button>
            </label>

            {error && <p className="login-error" role="alert">{error}</p>}

            <button className="login-submit" type="submit" disabled={enviando}>
              {enviando ? 'INGRESANDO…' : 'INGRESAR'}
              {!enviando && <LogIn aria-hidden="true" />}
            </button>
          </form>

          <div className="login-pie-form">
            <div className="login-pie-linea"><span>♨</span></div>
            <p>Sistema de gestión <b>•</b> Tú o Tú</p>
          </div>
          <div className="login-decor login-decor-inferior" aria-hidden="true">✦</div>
        </div>
      </section>
    </main>
  );
}
