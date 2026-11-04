// Cliente mínimo para la API REST. Agrega el token a cada petición.
const BASE = '/api';

export function obtenerToken() {
  return localStorage.getItem('token');
}

// Abre un PDF protegido (necesita el token): lo descarga y lo muestra en otra pestaña.
export async function abrirPdf(ruta) {
  const ventana = window.open('', '_blank');
  const cabeceras = {};
  const token = obtenerToken();
  if (token) cabeceras.Authorization = `Bearer ${token}`;
  const respuesta = await fetch(BASE + ruta, { headers: cabeceras });
  if (!respuesta.ok) {
    if (ventana) ventana.close();
    const error = new Error('No se pudo abrir la boleta.');
    error.estado = respuesta.status;
    throw error;
  }
  const direccion = URL.createObjectURL(await respuesta.blob());
  if (ventana) ventana.location.href = direccion;
  else window.location.assign(direccion);
}

export async function api(ruta, { metodo = 'GET', cuerpo } = {}) {
  const cabeceras = { 'Content-Type': 'application/json' };
  const token = obtenerToken();
  if (token) cabeceras.Authorization = `Bearer ${token}`;

  const respuesta = await fetch(BASE + ruta, {
    method: metodo,
    headers: cabeceras,
    body: cuerpo ? JSON.stringify(cuerpo) : undefined,
  });

  if (respuesta.status === 204) return null;

  let datos = null;
  try {
    datos = await respuesta.json();
  } catch {
    datos = null;
  }
  if (respuesta.status === 401 && !ruta.startsWith('/auth/login')) {
    // La sesión ya no es válida (por ejemplo, el servidor se reinició o el usuario fue desactivado).
    localStorage.removeItem('token');
    localStorage.removeItem('usuario');
    window.location.assign('/ingreso');
  }
  if (!respuesta.ok) {
    const error = new Error(datos?.mensaje || 'Ocurrió un error. Inténtelo de nuevo.');
    error.estado = respuesta.status;
    throw error;
  }
  return datos;
}
