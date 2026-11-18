import { Link } from 'react-router-dom';
import { useAuth } from '../modulos/seguridad/auth.jsx';
import Dashboard from '../modulos/gerente/Dashboard.jsx';
import InicioMozo from '../modulos/pedidos/InicioMozo.jsx';
import InicioCajero from '../modulos/ventas/InicioCajero.jsx';

const ATAJOS = [
  { ruta: '/pedidos/nuevo', texto: 'Registrar un pedido', permiso: 'PEDIDOS_REGISTRAR' },
  { ruta: '/pedidos', texto: 'Ver el estado de los pedidos', permiso: 'PEDIDOS_VER' },
  { ruta: '/caja', texto: 'Ir a la caja', permiso: 'VENTAS_REGISTRAR' },
];

// El gerente ve su panel con indicadores; mozos y cajeros ven accesos directos a lo que pueden hacer.
export default function Inicio() {
  const { usuario, tienePermiso } = useAuth();
  if (tienePermiso('REPORTES_VER')) return <Dashboard />;
  if (usuario.rol === 'MOZO') return <InicioMozo />;
  if (tienePermiso('VENTAS_REGISTRAR')) return <InicioCajero />;
  return (
    <div>
      <h2>Hola, {usuario.nombreCompleto}</h2>
      <p>Su rol es <strong>{usuario.rol}</strong>. Estas son las tareas que puede realizar:</p>
      <div className="acciones">
        {ATAJOS.filter((a) => tienePermiso(a.permiso)).map((a) => (
          <Link key={a.ruta} className="boton" to={a.ruta}>{a.texto}</Link>
        ))}
      </div>
    </div>
  );
}
