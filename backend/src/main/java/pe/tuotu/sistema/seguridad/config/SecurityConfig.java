package pe.tuotu.sistema.seguridad.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
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
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // --- Sprint 1: seguridad ---
                        .requestMatchers("/api/auth/login", "/error").permitAll()
                        .requestMatchers("/api/usuarios/**", "/api/roles/**").hasAuthority("USUARIOS_GESTIONAR")
                        // --- Sprint 2: pedidos ---
                        .requestMatchers(HttpMethod.POST, "/api/pedidos").hasAuthority("PEDIDOS_REGISTRAR")
                        .requestMatchers(HttpMethod.PATCH, "/api/pedidos/*/estado").hasAuthority("PEDIDOS_ESTADO")
                        .requestMatchers(HttpMethod.GET, "/api/pedidos/**").hasAuthority("PEDIDOS_VER")
                        .requestMatchers("/api/carta").hasAnyAuthority("PEDIDOS_REGISTRAR", "PEDIDOS_VER")
                        // --- Sprint 3: inventario ---
                        .requestMatchers("/api/insumos/**", "/api/productos/**").hasAuthority("INVENTARIO_GESTIONAR")
                        // --- Sprint 4: ventas, comprobantes y reportes ---
                        .requestMatchers("/api/ventas/**", "/api/comprobantes/**").hasAuthority("VENTAS_REGISTRAR")
                        .requestMatchers("/api/reportes/**").hasAuthority("REPORTES_VER")
                        // --- cualquier otra ruta exige haber iniciado sesión ---
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

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
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
