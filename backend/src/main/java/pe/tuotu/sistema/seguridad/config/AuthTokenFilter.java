package pe.tuotu.sistema.seguridad.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import pe.tuotu.sistema.seguridad.dominio.Usuario;
import pe.tuotu.sistema.seguridad.repositorio.SesionTokenRepository;
import pe.tuotu.sistema.seguridad.util.TokenUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Lee el encabezado "Authorization: Bearer ..." y, si el token corresponde a una sesión
 * no revocada de un usuario activo, deja al usuario autenticado con los permisos de su rol.
 */
public class AuthTokenFilter extends OncePerRequestFilter {

    private final SesionTokenRepository sesiones;

    public AuthTokenFilter(SesionTokenRepository sesiones) {
        this.sesiones = sesiones;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String cabecera = request.getHeader("Authorization");
        if (cabecera != null && cabecera.startsWith("Bearer ")) {
            String token = cabecera.substring(7).trim();
            sesiones.findByTokenHashAndRevocadoFalse(TokenUtil.hash(token)).ifPresent(sesion -> {
                Usuario usuario = sesion.getUsuario();
                if (usuario.isActivo()) {
                    List<GrantedAuthority> autoridades = new ArrayList<>();
                    autoridades.add(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().getNombre()));
                    usuario.getRol().getPermisos()
                            .forEach(p -> autoridades.add(new SimpleGrantedAuthority(p.getCodigo())));
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(usuario.getNombreUsuario(), null, autoridades));
                }
            });
        }
        chain.doFilter(request, response);
    }
}
