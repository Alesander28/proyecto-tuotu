package pe.tuotu.sistema.inventario.dominio;

/** Situación del stock de un insumo respecto a su stock mínimo. */
public enum EstadoStock {
    AGOTADO,
    BAJO,
    PROXIMO,
    NORMAL
}
