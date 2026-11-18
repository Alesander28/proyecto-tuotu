import { useEffect, useState } from 'react';
import { api } from '../../comun/api.js';
import { useAuth } from './auth.jsx';
import { Users, UserPlus, Search, ShieldCheck, CircleCheck, CircleX, Pencil, UserRound, LockKeyhole, X } from 'lucide-react';
import './usuarios.css';

const VACIO = { nombreUsuario: '', nombreCompleto: '', rolId: '', contrasena: '' };
const iniciales = (nombre = '') => nombre.trim().split(/\s+/).filter(Boolean).slice(0, 2).map(p => p[0]?.toUpperCase()).join('') || 'U';
const claseRol = (rol = '') => rol.toLowerCase().replace(/[^a-z0-9]+/g, '-');

// HU-002 (registrar), HU-003 (rol), HU-004 (actualizar) y HU-005 (desactivar).
export default function Usuarios() {
  const { tienePermiso } = useAuth();
  const [usuarios, setUsuarios] = useState([]);
  const [roles, setRoles] = useState([]);
  const [formulario, setFormulario] = useState(null);
  const [editandoId, setEditandoId] = useState(null);
  const [error, setError] = useState('');
  const [aviso, setAviso] = useState('');
  const [busqueda, setBusqueda] = useState('');
  const [filtroRol, setFiltroRol] = useState('todos');
  const [filtroEstado, setFiltroEstado] = useState('todos');
  const [guardando, setGuardando] = useState(false);

  async function cargar() {
    try {
      const [u, r] = await Promise.all([api('/usuarios'), api('/roles')]);
      setUsuarios(u);
      setRoles(r);
      setError('');
    } catch (e) { setError(e.message); }
  }

  useEffect(() => { if (tienePermiso('USUARIOS_GESTIONAR')) cargar(); }, []);

  if (!tienePermiso('USUARIOS_GESTIONAR')) {
    return <p className="error">No tiene permiso para gestionar usuarios.</p>;
  }

  const activos = usuarios.filter(u => u.activo).length;
  const inactivos = usuarios.length - activos;
  const porRol = roles.map(r => ({ ...r, cantidad: usuarios.filter(u => u.rolId === r.id).length }));
  const filtrados = usuarios.filter(u => {
    const texto = `${u.nombreUsuario} ${u.nombreCompleto} ${u.rol}`.toLowerCase();
    return texto.includes(busqueda.toLowerCase()) && (filtroRol === 'todos' || String(u.rolId) === filtroRol) &&
      (filtroEstado === 'todos' || (filtroEstado === 'activos' ? u.activo : !u.activo));
  });

  function nuevo() {
    setEditandoId(null);
    setFormulario({ ...VACIO, rolId: roles[0]?.id ?? '' });
    setError(''); setAviso('');
  }
  function editar(u) {
    setEditandoId(u.id);
    setFormulario({ nombreUsuario: u.nombreUsuario, nombreCompleto: u.nombreCompleto, rolId: u.rolId, contrasena: '' });
    setError(''); setAviso('');
  }
  async function guardar(evento) {
    evento.preventDefault(); setError(''); setGuardando(true);
    try {
      if (editandoId) {
        await api(`/usuarios/${editandoId}`, { metodo: 'PUT', cuerpo: { nombreCompleto: formulario.nombreCompleto, rolId: Number(formulario.rolId), contrasena: formulario.contrasena } });
        setAviso('Los datos del usuario se actualizaron correctamente.');
      } else {
        await api('/usuarios', { metodo: 'POST', cuerpo: { ...formulario, rolId: Number(formulario.rolId) } });
        setAviso('Usuario registrado correctamente.');
      }
      setFormulario(null); await cargar();
    } catch (e) { setError(e.message); }
    finally { setGuardando(false); }
  }
  async function desactivar(u) {
    if (!window.confirm(`¿Desactivar a ${u.nombreUsuario}? Ya no podrá ingresar al sistema.`)) return;
    setError('');
    try { await api(`/usuarios/${u.id}/desactivar`, { metodo: 'PATCH' }); setAviso(`${u.nombreUsuario} fue desactivado.`); await cargar(); }
    catch (e) { setError(e.message); }
  }
  const cambiar = campo => e => setFormulario(prev => ({ ...prev, [campo]: e.target.value }));

  return (
    <section className="usuarios-modulo">
      <div className="usuarios-titulo fila-titulo">
        <div><h2>Usuarios</h2><p>Administra el personal, sus roles y el acceso al sistema.</p></div>
        <button className="boton usuarios-nuevo" onClick={nuevo}><UserPlus size={18}/> Nuevo usuario</button>
      </div>
      {error && <p className="error" role="alert">{error}</p>}
      {aviso && <p className="aviso usuarios-aviso" role="status">{aviso}</p>}

      <div className="usuarios-resumen">
        <article className="tarjeta usuarios-kpi"><span className="usuarios-kpi-icon naranja"><Users/></span><div><small>Total de usuarios</small><strong>{usuarios.length}</strong><span>Personal registrado</span></div></article>
        <article className="tarjeta usuarios-kpi"><span className="usuarios-kpi-icon verde"><CircleCheck/></span><div><small>Usuarios activos</small><strong>{activos}</strong><span>Con acceso habilitado</span></div></article>
        <article className="tarjeta usuarios-kpi"><span className="usuarios-kpi-icon rojo"><CircleX/></span><div><small>Desactivados</small><strong>{inactivos}</strong><span>Sin acceso al sistema</span></div></article>
        <article className="tarjeta usuarios-kpi"><span className="usuarios-kpi-icon cafe"><ShieldCheck/></span><div><small>Roles disponibles</small><strong>{roles.length}</strong><span>Perfiles de acceso</span></div></article>
      </div>

      <div className="usuarios-roles" aria-label="Resumen de usuarios por rol">
        {porRol.map(r => <div className="usuarios-rol-chip" key={r.id}><span className={`usuarios-rol-dot ${claseRol(r.nombre)}`}/><span>{r.nombre}</span><strong>{r.cantidad}</strong></div>)}
      </div>

      <div className="tarjeta usuarios-filtros">
        <label className="usuarios-busqueda"><Search size={18}/><input value={busqueda} onChange={e => setBusqueda(e.target.value)} placeholder="Buscar por nombre o usuario…" aria-label="Buscar usuario"/></label>
        <label className="usuarios-filtro"><span>Rol</span><select value={filtroRol} onChange={e => setFiltroRol(e.target.value)}><option value="todos">Todos los roles</option>{roles.map(r => <option key={r.id} value={r.id}>{r.nombre}</option>)}</select></label>
        <label className="usuarios-filtro"><span>Estado</span><select value={filtroEstado} onChange={e => setFiltroEstado(e.target.value)}><option value="todos">Todos los estados</option><option value="activos">Activos</option><option value="inactivos">Desactivados</option></select></label>
      </div>

      <section className="tarjeta usuarios-tabla-card">
        <div className="usuarios-tabla-cabecera"><div><h3>Personal del restaurante</h3><p>Revisa y administra las cuentas de acceso.</p></div><span className="usuarios-contador">{filtrados.length} usuario{filtrados.length !== 1 ? 's' : ''}</span></div>
        <div className="usuarios-tabla-scroll"><table className="tabla usuarios-tabla"><thead><tr><th>Usuario</th><th>Nombre completo</th><th>Rol</th><th>Estado</th><th>Acciones</th></tr></thead>
          <tbody>{filtrados.map(u => <tr key={u.id} className={!u.activo ? 'inactivo' : ''}>
            <td><div className="usuarios-identidad"><span className="usuarios-avatar">{iniciales(u.nombreCompleto)}</span><div><strong>{u.nombreUsuario}</strong><small>Cuenta #{u.id}</small></div></div></td>
            <td>{u.nombreCompleto}</td><td><span className={`usuarios-rol-badge ${claseRol(u.rol)}`}>{u.rol}</span></td>
            <td><span className={`usuarios-estado ${u.activo ? 'activo' : 'inactivo'}`}><span/>{u.activo ? 'Activo' : 'Desactivado'}</span></td>
            <td><div className="usuarios-acciones"><button className="usuarios-btn-accion" onClick={() => editar(u)} title="Editar usuario"><Pencil size={16}/>Editar</button>{u.activo && <button className="usuarios-btn-accion peligro" onClick={() => desactivar(u)} title="Desactivar usuario"><LockKeyhole size={16}/>Desactivar</button>}</div></td>
          </tr>)}</tbody></table>
          {filtrados.length === 0 && <div className="usuarios-vacio"><UserRound size={32}/><strong>No se encontraron usuarios</strong><span>Prueba cambiando los filtros o registra un nuevo usuario.</span></div>}
        </div>
      </section>

      {formulario && <div className="fondo-modal" role="presentation" onMouseDown={e => { if (e.target === e.currentTarget) setFormulario(null); }}>
        <form className="modal usuarios-modal" onSubmit={guardar}>
          <div className="modal-cabecera"><div className="usuarios-modal-titulo-icon"><UserPlus size={21}/></div><div className="usuarios-modal-heading"><h3>{editandoId ? 'Editar usuario' : 'Registrar usuario'}</h3><p>{editandoId ? 'Actualiza los datos y el rol de la cuenta.' : 'Crea una cuenta de acceso para el personal.'}</p></div><button type="button" className="boton-icono" aria-label="Cerrar" onClick={() => setFormulario(null)}><X size={18}/></button></div>
          {error && <p className="error" role="alert">{error}</p>}
          <div className="usuarios-form-grid"><label>Nombre de usuario<input value={formulario.nombreUsuario} onChange={cambiar('nombreUsuario')} disabled={!!editandoId} required minLength={3} maxLength={40} autoComplete="username" placeholder="Ej. maria.cajera"/><small>{editandoId ? 'El nombre de usuario no se puede cambiar.' : 'Debe ser único y tener al menos 3 caracteres.'}</small></label>
          <label>Nombre completo<input value={formulario.nombreCompleto} onChange={cambiar('nombreCompleto')} required maxLength={120} placeholder="Nombre y apellidos"/></label>
          <label>Rol de acceso<select value={formulario.rolId} onChange={cambiar('rolId')} required>{roles.map(r => <option key={r.id} value={r.id}>{r.nombre}</option>)}</select><small>El rol determina los permisos de la cuenta.</small></label>
          <label>{editandoId ? 'Cambiar contraseña (opcional)' : 'Contraseña'}<input type="password" value={formulario.contrasena} onChange={cambiar('contrasena')} required={!editandoId} minLength={6} autoComplete="new-password" placeholder={editandoId ? 'Dejar vacío para mantener la actual' : 'Mínimo 6 caracteres'}/></label></div>
          <div className="usuarios-modal-pie"><button type="button" className="boton secundario" onClick={() => setFormulario(null)}>Cancelar</button><button type="submit" className="boton" disabled={guardando}>{guardando ? 'Guardando…' : editandoId ? 'Guardar cambios' : 'Crear usuario'}</button></div>
        </form>
      </div>}
    </section>
  );
}
