import { useEffect, useState } from 'react';
import { api } from '../../comun/api.js';
import { useAuth } from './auth.jsx';

const VACIO = { nombreUsuario: '', nombreCompleto: '', rolId: '', contrasena: '' };

// HU-002 (registrar), HU-003 (rol), HU-004 (actualizar) y HU-005 (desactivar).
export default function Usuarios() {
  const { tienePermiso } = useAuth();
  const [usuarios, setUsuarios] = useState([]);
  const [roles, setRoles] = useState([]);
  const [formulario, setFormulario] = useState(null); // null = oculto
  const [editandoId, setEditandoId] = useState(null);
  const [error, setError] = useState('');
  const [aviso, setAviso] = useState('');

  async function cargar() {
    try {
      const [u, r] = await Promise.all([api('/usuarios'), api('/roles')]);
      setUsuarios(u);
      setRoles(r);
    } catch (e) {
      setError(e.message);
    }
  }

  useEffect(() => {
    if (tienePermiso('USUARIOS_GESTIONAR')) cargar();
  }, []);

  if (!tienePermiso('USUARIOS_GESTIONAR')) {
    return <p className="error">No tiene permiso para gestionar usuarios.</p>;
  }

  function nuevo() {
    setEditandoId(null);
    setFormulario({ ...VACIO, rolId: roles[0]?.id ?? '' });
    setError('');
    setAviso('');
  }

  function editar(u) {
    setEditandoId(u.id);
    setFormulario({ nombreUsuario: u.nombreUsuario, nombreCompleto: u.nombreCompleto, rolId: u.rolId, contrasena: '' });
    setError('');
    setAviso('');
  }

  async function guardar(evento) {
    evento.preventDefault();
    setError('');
    try {
      if (editandoId) {
        await api(`/usuarios/${editandoId}`, {
          metodo: 'PUT',
          cuerpo: { nombreCompleto: formulario.nombreCompleto, rolId: Number(formulario.rolId), contrasena: formulario.contrasena },
        });
        setAviso('Usuario actualizado.');
      } else {
        await api('/usuarios', {
          metodo: 'POST',
          cuerpo: { ...formulario, rolId: Number(formulario.rolId) },
        });
        setAviso('Usuario registrado.');
      }
      setFormulario(null);
      await cargar();
    } catch (e) {
      setError(e.message);
    }
  }

  async function desactivar(u) {
    if (!window.confirm(`¿Desactivar a ${u.nombreUsuario}? Ya no podrá ingresar al sistema.`)) return;
    setError('');
    try {
      await api(`/usuarios/${u.id}/desactivar`, { metodo: 'PATCH' });
      setAviso('Usuario desactivado.');
      await cargar();
    } catch (e) {
      setError(e.message);
    }
  }

  const cambiar = (campo) => (e) => setFormulario({ ...formulario, [campo]: e.target.value });

  return (
    <div>
      <div className="fila-titulo">
        <h2>Usuarios del personal</h2>
        <button className="boton" onClick={nuevo}>+ Nuevo usuario</button>
      </div>

      {error && <p className="error" role="alert">{error}</p>}
      {aviso && <p className="aviso" role="status">{aviso}</p>}

      {formulario && (
        <form className="tarjeta formulario" onSubmit={guardar}>
          <h3>{editandoId ? 'Editar usuario' : 'Nuevo usuario'}</h3>
          <label htmlFor="f-usuario">Usuario (único)</label>
          <input id="f-usuario" value={formulario.nombreUsuario} onChange={cambiar('nombreUsuario')}
                 disabled={!!editandoId} required minLength={3} maxLength={40} />

          <label htmlFor="f-nombre">Nombre completo</label>
          <input id="f-nombre" value={formulario.nombreCompleto} onChange={cambiar('nombreCompleto')} required />

          <label htmlFor="f-rol">Rol (obligatorio)</label>
          <select id="f-rol" value={formulario.rolId} onChange={cambiar('rolId')} required>
            {roles.map((r) => <option key={r.id} value={r.id}>{r.nombre}</option>)}
          </select>

          <label htmlFor="f-clave">{editandoId ? 'Nueva contraseña (opcional)' : 'Contraseña'}</label>
          <input id="f-clave" type="password" value={formulario.contrasena} onChange={cambiar('contrasena')}
                 required={!editandoId} minLength={editandoId ? undefined : 6} autoComplete="new-password" />

          <div className="acciones">
            <button className="boton" type="submit">Guardar</button>
            <button className="boton secundario" type="button" onClick={() => setFormulario(null)}>Cancelar</button>
          </div>
        </form>
      )}

      <table className="tabla">
        <thead>
          <tr><th>Usuario</th><th>Nombre</th><th>Rol</th><th>Estado</th><th>Acciones</th></tr>
        </thead>
        <tbody>
          {usuarios.map((u) => (
            <tr key={u.id} className={u.activo ? '' : 'inactivo'}>
              <td>{u.nombreUsuario}</td>
              <td>{u.nombreCompleto}</td>
              <td>{u.rol}</td>
              <td>{u.activo ? 'Activo' : 'Inactivo'}</td>
              <td>
                <button className="enlace" onClick={() => editar(u)}>Editar</button>
                {u.activo && <button className="enlace peligro" onClick={() => desactivar(u)}>Desactivar</button>}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
