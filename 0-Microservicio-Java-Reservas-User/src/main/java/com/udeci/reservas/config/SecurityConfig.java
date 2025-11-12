package com.udeci.reservas.config;


import com.udeci.reservas.service.DbUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  private final DbUserDetailsService userDetailsService;
  private final CustomAuthEntryPoint authEntryPoint;
  private final CustomAccessDeniedHandler accessDeniedHandler;

  public SecurityConfig(DbUserDetailsService userDetailsService,
                        CustomAuthEntryPoint authEntryPoint,
                        CustomAccessDeniedHandler accessDeniedHandler) {
    this.userDetailsService = userDetailsService;
    this.authEntryPoint = authEntryPoint;
    this.accessDeniedHandler = accessDeniedHandler;
  }

  // BCrypt como siempre
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  // Deja que Spring arme el AuthenticationManager con tu UserDetailsService + PasswordEncoder
  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
    return cfg.getAuthenticationManager();
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
      // Registra tu UDS para que el provider por defecto sea DAO con BCrypt
      .userDetailsService(userDetailsService)

      .cors(c -> c.configurationSource(corsConfigurationSource()))
      .csrf(csrf -> csrf.disable())
      .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

      .exceptionHandling(eh -> eh
        .authenticationEntryPoint(authEntryPoint)     // 401 JSON (sin popup)
        .accessDeniedHandler(accessDeniedHandler)     // 403 JSON
      )

      .authorizeHttpRequests(auth -> auth
        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // CORS preflight

        // Swagger + H2
        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/h2-console/**").permitAll()

        // Login/registro
        .requestMatchers("/auth/**").permitAll()

        // Reservas
        .requestMatchers(HttpMethod.GET, "/reservas/**").permitAll()
        .requestMatchers(HttpMethod.POST, "/reservas/**").hasAnyRole("USER", "ADMIN")
        .requestMatchers(HttpMethod.PUT, "/reservas/**").hasRole("ADMIN")
        .requestMatchers(HttpMethod.DELETE, "/reservas/**").hasRole("ADMIN")

        .anyRequest().authenticated()
      )

      .headers(h -> h.frameOptions(f -> f.disable())) // H2
      .httpBasic(b -> b.authenticationEntryPoint(authEntryPoint));

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration cfg = new CorsConfiguration();
    cfg.setAllowedOrigins(List.of("*"));
    cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    cfg.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
    cfg.setExposedHeaders(List.of("WWW-Authenticate"));
    cfg.setAllowCredentials(false); // con "*" debe ser false

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", cfg);
    return source;
  }
}
