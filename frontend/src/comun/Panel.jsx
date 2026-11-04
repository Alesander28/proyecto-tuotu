import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../modulos/seguridad/auth.jsx';

// Cada usuario ve solo las opciones que su rol permite (HU-003).
export default function Panel() {
  const { usuario, cerrarSesion, tienePermiso } = useAuth();
  const navegar = useNavigate();

  async function salir() {
    await cerrarSesion();
    navegar('/ingreso');
  }

  return (
    <div className="panel">
      <nav className="menu" aria-label="Menú principal">
        <div className="menu-titulo">Tú o Tú</div>
        <NavLink to="/" end>Inicio</NavLink>
        {tienePermiso('USUARIOS_GESTIONAR') && <NavLink to="/usuarios">Usuarios</NavLink>}
        {tienePermiso('PEDIDOS_REGISTRAR') && <NavLink to="/pedidos/nuevo">Nuevo pedido</NavLink>}
        {tienePermiso('PEDIDOS_VER') && <NavLink to="/pedidos" end>Pedidos</NavLink>}
        {tienePermiso('INVENTARIO_GESTIONAR') && <NavLink to="/productos">Productos</NavLink>}
        {tienePermiso('INVENTARIO_GESTIONAR') && <NavLink to="/inventario">Inventario</NavLink>}
        {tienePermiso('VENTAS_REGISTRAR') && <NavLink to="/caja">Caja</NavLink>}
        {tienePermiso('REPORTES_VER') && <NavLink to="/reportes">Reportes</NavLink>}
        <button type="button" className="menu-salir" onClick={salir}>Cerrar sesión</button>
      </nav>
      <section className="contenido">
        <header className="barra">
          <span>{usuario.nombreCompleto} · <strong>{usuario.rol}</strong></span>
        </header>
        <Outlet />
      </section>
    </div>
  );
}
