import { Navigate, Route, Routes } from 'react-router-dom';
import Panel from './comun/Panel.jsx';
import Inicio from './comun/Inicio.jsx';
// Sprint 1: seguridad
import { useAuth } from './modulos/seguridad/auth.jsx';
import Login from './modulos/seguridad/Login.jsx';
import Usuarios from './modulos/seguridad/Usuarios.jsx';
// Sprint 2: pedidos
import NuevoPedido from './modulos/pedidos/NuevoPedido.jsx';
import EstadoPedidos from './modulos/pedidos/EstadoPedidos.jsx';
// Sprint 3: inventario
import Inventario from './modulos/inventario/Inventario.jsx';
import Productos from './modulos/inventario/Productos.jsx';
// Sprint 4: ventas y reportes
import Caja from './modulos/ventas/Caja.jsx';
import Reportes from './modulos/reportes/Reportes.jsx';
// Módulos de administración del gerente
import Mesas from './modulos/gerente/Mesas.jsx';
import MesasMozo from './modulos/pedidos/MesasMozo.jsx';
import Cajas from './modulos/gerente/Cajas.jsx';
import Auditoria from './modulos/gerente/Auditoria.jsx';

function Protegida({ children }) {
  const { usuario } = useAuth();
  return usuario ? children : <Navigate to="/ingreso" replace />;
}

function ConPermiso({ permiso, children }) {
  const { tienePermiso } = useAuth();
  return tienePermiso(permiso) ? children : <Navigate to="/" replace />;
}

function MesasPorRol() {
  const { tienePermiso } = useAuth();
  if (tienePermiso('MESAS_GESTIONAR')) return <Mesas />;
  if (tienePermiso('PEDIDOS_REGISTRAR')) return <MesasMozo />;
  return <Navigate to="/" replace />;
}

export default function App() {
  return (
    <Routes>
      <Route path="/ingreso" element={<Login />} />
      <Route
        path="/"
        element={
          <Protegida>
            <Panel />
          </Protegida>
        }
      >
        <Route index element={<Inicio />} />
        {/* Sprint 1 */}
        <Route path="usuarios" element={<Usuarios />} />
        {/* Sprint 2 */}
        <Route path="pedidos/nuevo" element={<NuevoPedido />} />
        <Route path="pedidos" element={<EstadoPedidos />} />
        {/* Sprint 3 */}
        <Route path="inventario" element={<Inventario />} />
        <Route path="productos" element={<Productos />} />
        {/* Sprint 4 */}
        <Route path="caja" element={<Caja />} />
        <Route path="reportes" element={<Reportes />} />
        {/* Administración del gerente */}
        <Route path="mesas" element={<MesasPorRol />} />
        <Route path="cajas" element={<ConPermiso permiso="CAJAS_VER"><Cajas /></ConPermiso>} />
        <Route path="auditoria" element={<ConPermiso permiso="AUDITORIA_VER"><Auditoria /></ConPermiso>} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
