package pe.tuotu.sistema.auditoria.evento;

/**
 * Evento genérico para dejar constancia de una operación sensible (usuarios, productos, inventario, cajas...).
 * Quien lo publica no conoce a los observadores: la auditoría lo guarda y otros observadores podrían reaccionar.
 */
public record OperacionAuditada(String modulo, String tipo, String usuario, String descripcion, String referencia) {
}
