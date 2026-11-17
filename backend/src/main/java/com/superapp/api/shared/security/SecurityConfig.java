package com.superapp.api.shared.security;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * Seguridad de la API (NFR-04): sin sesiones en el servidor, cada petición trae
 * un JWT en "Authorization: Bearer ...". Aquí están TODAS las reglas por rol.
 * Sin token -> 401, rol incorrecto -> 403 (ambos en JSON).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** Costo de BCrypt: 2^10 rondas (la rúbrica pide >= 10). */
    public static final int BCRYPT_STRENGTH = 10;

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(BCRYPT_STRENGTH);
    }

    @Bean
    SecretKey jwtKey(@Value("${superapp.jwt.secret}") String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos 32 caracteres (HS256)");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtKey));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtKey) {
        return NimbusJwtDecoder.withSecretKey(jwtKey).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JsonSecurityErrors errors) throws Exception {
        // El claim "roles" del token se vuelve ROLE_CUSTOMER, ROLE_SELLER...
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("roles");
        roles.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roles);

        http
            .csrf(csrf -> csrf.disable()) // API sin cookies: el token va en un header
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .httpBasic(b -> b.disable())
            .formLogin(f -> f.disable())
            .authorizeHttpRequests(auth -> auth
                // Público
                .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                .requestMatchers("/api/health").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/listings/mine").hasRole("SELLER")
                .requestMatchers(HttpMethod.GET, "/api/listings", "/api/listings/*", "/api/payments/methods").permitAll()
                // Administración
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // Vendedor: publicar, editar, eliminar
                .requestMatchers(HttpMethod.POST, "/api/listings").hasRole("SELLER")
                .requestMatchers(HttpMethod.PUT, "/api/listings/**").hasRole("SELLER")
                .requestMatchers(HttpMethod.DELETE, "/api/listings/**").hasRole("SELLER")
                // Cliente: comprar, pagar, billetera, pedidos
                .requestMatchers("/api/payments/**", "/api/wallet/**", "/api/cart/**", "/api/orders/**").hasRole("CUSTOMER")
                .requestMatchers(HttpMethod.POST, "/api/deliveries").hasRole("CUSTOMER")
                .requestMatchers("/api/deliveries/mine").hasRole("CUSTOMER")
                // Conductor
                .requestMatchers("/api/deliveries/available", "/api/deliveries/assigned").hasRole("DRIVER")
                // Usuarios: cada quien su perfil; ver a otros solo ADMIN
                .requestMatchers("/api/users/me", "/api/users/me/**").authenticated()
                .requestMatchers("/api/users/**").hasRole("ADMIN")
                // Cualquier otra ruta de la API pide sesión
                .requestMatchers("/api/**").authenticated()
                // La página (index.html y recursos estáticos) es pública
                .anyRequest().permitAll())
            .oauth2ResourceServer(o -> o
                .jwt(jwt -> jwt.jwtAuthenticationConverter(converter))
                .authenticationEntryPoint(errors)
                .accessDeniedHandler(errors))
            .exceptionHandling(e -> e
                .authenticationEntryPoint(errors)
                .accessDeniedHandler(errors));
        return http.build();
    }
}
