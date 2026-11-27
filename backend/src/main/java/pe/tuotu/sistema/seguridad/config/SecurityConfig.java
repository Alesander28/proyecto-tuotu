package pe.tuotu.sistema.seguridad.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import pe.tuotu.sistema.seguridad.repositorio.SesionTokenRepository;
import java.io.IOException;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filtros(HttpSecurity http, SesionTokenRepository sesiones) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        // --- Sprint 1: seguridad ---
                        .requestMatchers("/api/auth/login", "/error").permitAll()
                        .requestMatchers("/api/auth/logout", "/api/auth/me").authenticated()
                        .requestMatchers("/api/usuarios/**", "/api/roles/**").hasAuthority("USUARIOS_GESTIONAR")
                        // --- Sprint 2: pedidos ---
                        .requestMatchers(HttpMethod.POST, "/api/pedidos").hasAuthority("PEDIDOS_REGISTRAR")
                        .requestMatchers(HttpMethod.PATCH, "/api/pedidos/*/estado").hasAuthority("PEDIDOS_ESTADO")
                        // Búsqueda avanzada de pedidos: solo Gerente (REPORTES_VER).
                        .requestMatchers(HttpMethod.GET, "/api/pedidos/buscar").hasAuthority("REPORTES_VER")
                        .requestMatchers(HttpMethod.GET, "/api/pedidos/**").hasAuthority("PEDIDOS_VER")
                        .requestMatchers("/api/carta").hasAnyAuthority("PEDIDOS_REGISTRAR", "PEDIDOS_VER")
                        // --- Sprint 3: inventario ---
                        .requestMatchers("/api/insumos/**", "/api/productos/**").hasAuthority("INVENTARIO_GESTIONAR")
                        // --- Sprint 4: ventas, comprobantes y reportes ---
                        .requestMatchers(HttpMethod.GET, "/api/ventas/por-pedido/*").hasAnyAuthority("VENTAS_REGISTRAR", "REPORTES_VER")
                        .requestMatchers("/api/ventas/**", "/api/comprobantes/**").hasAuthority("VENTAS_REGISTRAR")
                        .requestMatchers("/api/reportes/**").hasAuthority("REPORTES_VER")
                        // --- Módulo del gerente: auditoría, mesas y cajas ---
                        .requestMatchers(HttpMethod.GET, "/api/auditoria/pedidos/*").hasAuthority("PEDIDOS_VER")
                        .requestMatchers("/api/auditoria/**").hasAuthority("AUDITORIA_VER")
                        .requestMatchers(HttpMethod.GET, "/api/mesas/*/historial").hasAnyAuthority("PEDIDOS_VER", "MESAS_GESTIONAR")
                        .requestMatchers(HttpMethod.GET, "/api/mesas").hasAnyAuthority("PEDIDOS_REGISTRAR", "MESAS_GESTIONAR")
                        // Administración exclusiva del gerente. Se declaran las rutas exactas
                        // para que POST /api/mesas no dependa del patrón comodín /**.
                        .requestMatchers(HttpMethod.POST, "/api/mesas").hasAuthority("MESAS_GESTIONAR")
                        .requestMatchers(HttpMethod.PATCH, "/api/mesas/*/activa").hasAuthority("MESAS_GESTIONAR")
                        .requestMatchers("/api/mesas/**").hasAuthority("MESAS_GESTIONAR")
                        .requestMatchers("/api/cajas/actual", "/api/cajas/abrir", "/api/cajas/cerrar").hasAuthority("CAJAS_OPERAR")
                        .requestMatchers("/api/cajas/**").hasAuthority("CAJAS_VER")
                        // Cierre seguro: toda API nueva debe declararse arriba con su permiso explícito.
                        .requestMatchers("/api/**").denyAll()
                        // Rutas no API (por ejemplo, recursos internos) requieren sesión.
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> escribir(res, 401, "Debe iniciar sesión."))
                        .accessDeniedHandler((req, res, ex) -> escribir(res, 403, "No tiene permiso para realizar esta acción.")))
                .addFilterBefore(new AuthTokenFilter(sesiones), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Value("${app.cors.allowed-origins:http://localhost:5173}")
    private String origenesPermitidos;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origenes = java.util.Arrays.stream(origenesPermitidos.split(","))
                .map(String::trim).filter(origen -> !origen.isEmpty()).toList();
        config.setAllowedOrigins(origenes);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin"));
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/api/**", config);
        return fuente;
    }

    private static void escribir(HttpServletResponse res, int estado, String mensaje) throws IOException {
        res.setStatus(estado);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write("{\"mensaje\":\"" + mensaje + "\"}");
    }
}
