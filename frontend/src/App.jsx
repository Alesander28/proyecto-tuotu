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

function Protegida({ children }) {
  const { usuario } = useAuth();
  return usuario ? children : <Navigate to="/ingreso" replace />;
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
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
