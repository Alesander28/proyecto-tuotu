package pe.tuotu.sistema.ventas.servicio;

import pe.tuotu.sistema.ventas.dominio.Comprobante;

/**
 * Contrato que el sistema necesita para producir el documento de un comprobante. Es la interfaz "objetivo" del
 * patrón Adapter: el resto del sistema depende de este contrato y no de una biblioteca de PDF en particular.
 */
@FunctionalInterface
public interface GeneradorComprobante {

    byte[] generar(Comprobante comprobante);
}
