package es.pedro.pymes_backend.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class SecurityConfig {

    private static final String ADMIN_ROLE = "ADMIN";
    private static final String PROFESSIONAL_ROLE = "PROFESSIONAL";
    private static final String GUEST_ROLE = "GUEST";

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final String[] allowedOrigins;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            CustomAuthenticationEntryPoint authenticationEntryPoint,
            CustomAccessDeniedHandler accessDeniedHandler,
            @Value("${app.cors.allowed-origins}") String[] allowedOrigins
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.allowedOrigins = allowedOrigins;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/guest", "/api/auth/refresh").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/me").hasAnyRole(ADMIN_ROLE, PROFESSIONAL_ROLE, GUEST_ROLE)

                        .requestMatchers(HttpMethod.GET, "/api/appointments").hasAnyRole(ADMIN_ROLE, PROFESSIONAL_ROLE, GUEST_ROLE)
                        .requestMatchers("/api/appointments", "/api/appointments/**").hasAnyRole(ADMIN_ROLE, PROFESSIONAL_ROLE)

                        .requestMatchers(HttpMethod.GET, "/api/clients").hasAnyRole(ADMIN_ROLE, PROFESSIONAL_ROLE, GUEST_ROLE)
                        .requestMatchers("/api/clients", "/api/clients/**").hasAnyRole(ADMIN_ROLE, PROFESSIONAL_ROLE)

                        .requestMatchers(HttpMethod.GET, "/api/services").hasAnyRole(ADMIN_ROLE, PROFESSIONAL_ROLE, GUEST_ROLE)
                        .requestMatchers("/api/services", "/api/services/**").hasAnyRole(ADMIN_ROLE, PROFESSIONAL_ROLE)

                        .requestMatchers(HttpMethod.GET, "/api/inventory/products").hasAnyRole(ADMIN_ROLE, PROFESSIONAL_ROLE, GUEST_ROLE)
                        .requestMatchers(HttpMethod.GET, "/api/inventory/products/**").hasAnyRole(ADMIN_ROLE, PROFESSIONAL_ROLE)
                        .requestMatchers(HttpMethod.POST, "/api/inventory/products").hasAnyRole(ADMIN_ROLE, PROFESSIONAL_ROLE)
                        .requestMatchers(HttpMethod.PUT, "/api/inventory/products/**").hasAnyRole(ADMIN_ROLE, PROFESSIONAL_ROLE)
                        .requestMatchers(HttpMethod.DELETE, "/api/inventory/products/**").hasRole(ADMIN_ROLE)

                        .requestMatchers(HttpMethod.GET, "/api/categories", "/api/categories/**").hasAnyRole(ADMIN_ROLE, PROFESSIONAL_ROLE)
                        .requestMatchers("/api/categories", "/api/categories/**").hasRole(ADMIN_ROLE)

                        .requestMatchers("/api/sales", "/api/sales/**").hasAnyRole(ADMIN_ROLE, PROFESSIONAL_ROLE)
                        .requestMatchers("/api/expenses", "/api/expenses/**").hasAnyRole(ADMIN_ROLE, PROFESSIONAL_ROLE)

                        .requestMatchers(HttpMethod.GET, "/api/professionals").hasAnyRole(ADMIN_ROLE, PROFESSIONAL_ROLE, GUEST_ROLE)
                        .requestMatchers("/api/professionals", "/api/professionals/**").hasRole(ADMIN_ROLE)

                        .requestMatchers("/api/reports", "/api/reports/**").hasRole(ADMIN_ROLE)
                        .requestMatchers("/api/settings", "/api/settings/**").hasRole(ADMIN_ROLE)

                        .anyRequest().hasRole(ADMIN_ROLE)
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException("No se utiliza UserDetailsService en la autenticacion JWT actual");
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
