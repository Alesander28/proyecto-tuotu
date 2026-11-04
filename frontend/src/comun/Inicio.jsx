import { useAuth } from '../modulos/seguridad/auth.jsx';

export default function Inicio() {
  const { usuario } = useAuth();
  return (
    <div>
      <h2>Bienvenido, {usuario.nombreCompleto}</h2>
      <p>Su rol es <strong>{usuario.rol}</strong>. Las opciones del menú corresponden a lo que puede hacer en el sistema.</p>
    </div>
  );
}
