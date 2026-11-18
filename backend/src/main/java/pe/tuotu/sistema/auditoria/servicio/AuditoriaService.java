package pe.tuotu.sistema.auditoria.servicio;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.tuotu.sistema.auditoria.dominio.RegistroAuditoria;
import pe.tuotu.sistema.auditoria.dto.RegistroAuditoriaDto;
import pe.tuotu.sistema.auditoria.repositorio.AuditoriaRepository;
import pe.tuotu.sistema.comun.ApiException;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/** Consulta de la auditoría. Solo lectura: no existen operaciones para modificar ni borrar registros. */
@Service
public class AuditoriaService {

    private static final int MAXIMO = 500;

    private final AuditoriaRepository auditoria;

    public AuditoriaService(AuditoriaRepository auditoria) {
        this.auditoria = auditoria;
    }

    /** Sin fechas, devuelve los últimos 30 días. Los demás filtros son opcionales. */
    @Transactional(readOnly = true)
    public List<RegistroAuditoriaDto> consultar(String modulo, String usuario, String texto, LocalDate desde, LocalDate hasta) {
        LocalDate fin = hasta != null ? hasta : LocalDate.now();
        LocalDate inicio = desde != null ? desde : fin.minusDays(29);
        if (fin.isBefore(inicio)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La fecha final no puede ser anterior a la fecha inicial.");
        }
        String m = minuscula(modulo);
        String u = minuscula(usuario);
        String q = minuscula(texto);
        return auditoria.findByFechaHoraGreaterThanEqualAndFechaHoraLessThanOrderByFechaHoraDesc(
                        inicio.atStartOfDay(), fin.plusDays(1).atStartOfDay()).stream()
                .filter(r -> m.isEmpty() || r.getModulo().toLowerCase(Locale.ROOT).equals(m))
                .filter(r -> u.isEmpty() || r.getUsuario().toLowerCase(Locale.ROOT).contains(u))
                .filter(r -> q.isEmpty() || r.getDescripcion().toLowerCase(Locale.ROOT).contains(q)
                        || (r.getReferencia() != null && r.getReferencia().toLowerCase(Locale.ROOT).contains(q)))
                .limit(MAXIMO)
                .map(AuditoriaService::aDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RegistroAuditoriaDto> recientes() {
        return auditoria.findTop10ByOrderByFechaHoraDesc().stream().map(AuditoriaService::aDto).toList();
    }

    @Transactional(readOnly = true)
    public List<RegistroAuditoriaDto> historialDePedido(String codigo) {
        return auditoria.findByReferenciaOrderByFechaHoraAsc(codigo).stream()
                .filter(r -> r.getModulo().equals("Pedidos") || r.getModulo().equals("Ventas"))
                .map(AuditoriaService::aDto).toList();
    }

    private static String minuscula(String texto) {
        return texto == null ? "" : texto.trim().toLowerCase(Locale.ROOT);
    }

    private static RegistroAuditoriaDto aDto(RegistroAuditoria r) {
        return new RegistroAuditoriaDto(r.getId(), r.getFechaHora(), r.getUsuario(), r.getModulo(), r.getTipo(),
                r.getDescripcion(), r.getReferencia());
    }
}
