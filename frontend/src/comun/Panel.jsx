import { useCallback, useEffect, useState } from 'react';
import { NavLink, Outlet, Link, useLocation, useNavigate } from 'react-router-dom';
import {
  Armchair, Banknote, Bell, ChartColumn, CirclePlus, ClipboardList, House, LogOut, Menu, Package, PanelLeftClose,
  PanelLeftOpen, Pizza, Receipt, ShieldCheck, User, Users, Wifi, WifiOff,
} from 'lucide-react';
import { useAuth } from '../modulos/seguridad/auth.jsx';
import { api } from './api.js';
import { capitalizar, fechaLarga } from './formato.js';

// Cada opción se muestra solo si el rol tiene el permiso. El servidor vuelve a validarlo en cada petición.
const OPCIONES = [
  { ruta: '/', etiqueta: 'Inicio', icono: House, fin: true },
  { ruta: '/pedidos', etiqueta: 'Pedidos', icono: ClipboardList, permiso: 'PEDIDOS_VER', fin: true },
  { ruta: '/pedidos/nuevo', etiqueta: 'Nuevo pedido', icono: CirclePlus, permiso: 'PEDIDOS_REGISTRAR' },
  { ruta: '/productos', etiqueta: 'Productos', icono: Pizza, permiso: 'INVENTARIO_GESTIONAR' },
  { ruta: '/inventario', etiqueta: 'Inventario', icono: Package, permiso: 'INVENTARIO_GESTIONAR' },
  { ruta: '/caja', etiqueta: 'Caja', icono: Receipt, permiso: 'VENTAS_REGISTRAR' },
  { ruta: '/reportes', etiqueta: 'Reportes', icono: ChartColumn, permiso: 'REPORTES_VER' },
  { ruta: '/usuarios', etiqueta: 'Usuarios', icono: Users, permiso: 'USUARIOS_GESTIONAR' },
  { ruta: '/mesas', etiqueta: 'Mesas', icono: Armchair, permisos: ['MESAS_GESTIONAR', 'PEDIDOS_REGISTRAR'] },
  { ruta: '/cajas', etiqueta: 'Cajas', icono: Banknote, permiso: 'CAJAS_VER' },
  { ruta: '/auditoria', etiqueta: 'Auditoría', icono: ShieldCheck, permiso: 'AUDITORIA_VER' },
];

// Cada minuto consulta al servidor: sirve de indicador de estado y de fuente de las notificaciones del gerente.
function useServidor(conAlertas) {
  const [enLinea, setEnLinea] = useState(true);
  const [alertas, setAlertas] = useState([]);
  const consultar = useCallback(async () => {
    // La conexión se comprueba con un endpoint común a todos los roles.
    // Un error de permisos en reportes no debe mostrar falsamente "Sin conexión".
    try {
      await api('/auth/me');
      setEnLinea(true);
    } catch {
      setEnLinea(false);
      return;
    }

    if (!conAlertas) return;

    try {
      const r = await api('/reportes/resumen');
      const lista = [];
      if (r.insumosAgotados > 0) lista.push({ texto: `${r.insumosAgotados} insumo(s) agotado(s)`, ruta: '/inventario' });
      if (r.insumosStockBajo - r.insumosAgotados > 0) lista.push({ texto: `${r.insumosStockBajo - r.insumosAgotados} insumo(s) con stock bajo`, ruta: '/inventario' });
      if (r.pedidosPendientes > 0) lista.push({ texto: `${r.pedidosPendientes} pedido(s) pendiente(s)`, ruta: '/pedidos' });
      setAlertas(lista);
    } catch {
      // Si falla solo el resumen, se conserva el estado real de conexión.
      setAlertas([]);
    }
  }, [conAlertas]);
  useEffect(() => {
    consultar();
    const reloj = setInterval(consultar, 60000);
    return () => clearInterval(reloj);
  }, [consultar]);
  return { enLinea, alertas };
}

export default function Panel() {
  const { usuario, cerrarSesion, tienePermiso } = useAuth();
  const navegar = useNavigate();
  const lugar = useLocation();
  const [colapsado, setColapsado] = useState(() => localStorage.getItem('menu-colapsado') === '1');
  const [menuAbierto, setMenuAbierto] = useState(false);
  const [campana, setCampana] = useState(false);
  const { enLinea, alertas } = useServidor(tienePermiso('REPORTES_VER'));

  useEffect(() => {
    setMenuAbierto(false);
    setCampana(false);
  }, [lugar.pathname]);

  function alternarMenu() {
    const nuevo = !colapsado;
    setColapsado(nuevo);
    localStorage.setItem('menu-colapsado', nuevo ? '1' : '0');
  }

  async function salir() {
    await cerrarSesion();
    navegar('/ingreso');
  }

  const clases = ['app', colapsado ? 'colapsado' : '', menuAbierto ? 'menu-abierto' : ''].join(' ').trim();

  return (
    <div className={clases}>
      <aside className="barra-lateral" aria-label="Navegación principal">
        <div className="marca">
          <img className="marca-logo" src="/logo-tuotu.png" alt="Logo de Tú o Tú" />
          <div className="marca-texto">
            <strong>TÚ O TÚ</strong>
            <span>Pizzas · Tacos · Parrillas</span>
          </div>
        </div>
        <nav className="menu-nav">
          {OPCIONES.filter((o) => (!o.permiso && !o.permisos) || (o.permiso && tienePermiso(o.permiso)) || (o.permisos && o.permisos.some(tienePermiso))).map(({ ruta, etiqueta, icono: Icono, fin }) => (
            <NavLink key={ruta} to={ruta} end={fin} className="item-menu" title={etiqueta}>
              <Icono size={20} aria-hidden="true" />
              <span className="item-texto">{etiqueta}</span>
            </NavLink>
          ))}
        </nav>
        <div className="menu-pie">
          <button type="button" className="item-menu" onClick={alternarMenu} title={colapsado ? 'Expandir menú' : 'Contraer menú'}>
            {colapsado ? <PanelLeftOpen size={20} /> : <PanelLeftClose size={20} />}
            <span className="item-texto">{colapsado ? 'Expandir' : 'Contraer'}</span>
          </button>
          <button type="button" className="item-menu" onClick={salir} title="Cerrar sesión">
            <LogOut size={20} aria-hidden="true" />
            <span className="item-texto">Cerrar sesión</span>
          </button>
        </div>
      </aside>
      <div className="velo" onClick={() => setMenuAbierto(false)} aria-hidden="true" />

      <div className="cuerpo">
        <header className="encabezado">
          <button type="button" className="boton-icono boton-menu-movil" onClick={() => setMenuAbierto(true)} aria-label="Abrir menú">
            <Menu size={20} />
          </button>
          <div className="encabezado-saludo">
            <h1>{usuario.rol === 'GERENTE' ? 'Gerente' : `Bienvenido, ${capitalizar(usuario.rol)}`}</h1>
            <small>{fechaLarga()}</small>
          </div>
          <span className={`estado-sistema ${enLinea ? 'en-linea' : 'sin-conexion'}`} role="status">
            {enLinea ? <Wifi size={14} /> : <WifiOff size={14} />} {enLinea ? 'Sistema en línea' : 'Sin conexión'}
          </span>
          {tienePermiso('REPORTES_VER') && (
            <div style={{ position: 'relative' }}>
              <button type="button" className="boton-icono" onClick={() => setCampana(!campana)} aria-label={`Notificaciones (${alertas.length})`} aria-expanded={campana}>
                <Bell size={19} />
                {alertas.length > 0 && <span className="insignia-campana">{alertas.length}</span>}
              </button>
              {campana && (
                <div className="menu-desplegable" role="menu">
                  <h4>Notificaciones</h4>
                  {alertas.length === 0 && <div className="nota" style={{ padding: 8 }}>No hay alertas por ahora.</div>}
                  {alertas.map((a) => <Link key={a.texto} to={a.ruta} role="menuitem">{a.texto}</Link>)}
                </div>
              )}
            </div>
          )}
          <div className="usuario-caja">
            <div className="usuario-avatar" aria-hidden="true"><User size={20} /></div>
            <div className="usuario-datos">
              <strong>{usuario.nombreCompleto}</strong>
              <span className="rol-chip">{usuario.rol}</span>
            </div>
          </div>
        </header>
        <main className="pagina">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
