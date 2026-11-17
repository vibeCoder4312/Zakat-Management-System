package com.iqra.zakat.config;

import com.iqra.zakat.security.AuthEntryPoint;
import com.iqra.zakat.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final AuthEntryPoint authEntryPoint;

    @Value("${app.cors.allowed-origins:http://localhost:*,http://127.0.0.1:*}")
    private String allowedOrigins;

    @Value("${app.security.swagger-public:false}")
    private boolean swaggerPublic;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authEntryPoint).accessDeniedHandler(authEntryPoint))
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    // Public endpoints - Allow CORS preflight, error, and login
                    auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                            .requestMatchers("/api/v1/auth/login", "/api/auth/login", "/auth/login").permitAll()
                            .requestMatchers("/error").permitAll()
                            .requestMatchers("/actuator/health", "/actuator/health/**").permitAll();

                    // Swagger / OpenAPI lockdown
                    if (swaggerPublic) {
                        auth.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll();
                    } else {
                        auth.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").hasRole("ADMIN");
                    }

                    // Authenticated password change
                    auth.requestMatchers("/api/v1/auth/change-password", "/api/auth/change-password", "/auth/change-password").authenticated();

                    // User administration (ADMIN only, before generic GET)
                    auth.requestMatchers("/api/v1/users/**", "/api/users/**").hasRole("ADMIN");

                    // Export endpoints (ADMIN and ACCOUNTANT)
                    auth.requestMatchers("/api/v1/export/**", "/api/export/**", "/api/exports/**").hasAnyRole("ADMIN", "ACCOUNTANT");

                    // Assessment mutations (ADMIN only)
                    auth.requestMatchers("/api/v1/assessments/**", "/api/assessments/**").hasRole("ADMIN");

                    // Planning mutations (ADMIN only)
                    auth.requestMatchers("/api/v1/planning/**", "/api/planning/**").hasRole("ADMIN");

                    // Master data mutations (ADMIN only)
                    auth.requestMatchers("/api/v1/classifications/**", "/api/classifications/**").hasRole("ADMIN");
                    auth.requestMatchers("/api/v1/beneficiary-categories/**", "/api/beneficiary-categories/**", "/api/categories/**").hasRole("ADMIN");
                    auth.requestMatchers("/api/v1/source-accounts/**", "/api/source-accounts/**").hasRole("ADMIN");
                    auth.requestMatchers("/api/v1/agents/**", "/api/agents/**").hasRole("ADMIN");
                    auth.requestMatchers("/api/v1/employees/**", "/api/employees/**").hasRole("ADMIN");
                    auth.requestMatchers("/api/v1/schools/**", "/api/schools/**").hasRole("ADMIN");

                    // Beneficiary and Allocation mutations (ADMIN, ACCOUNTANT)
                    auth.requestMatchers("/api/v1/beneficiaries/**", "/api/beneficiaries/**").hasAnyRole("ADMIN", "ACCOUNTANT");
                    auth.requestMatchers("/api/v1/allocations/**", "/api/allocations/**").hasAnyRole("ADMIN", "ACCOUNTANT");

                    // Read access to all authenticated users
                    auth.requestMatchers(HttpMethod.GET, "/api/v1/**", "/api/**").authenticated();

                    auth.anyRequest().authenticated();
                });

        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        config.setAllowedOriginPatterns(origins);
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept", "X-Requested-With", "Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers"));
        config.setExposedHeaders(Arrays.asList("Authorization", "Content-Disposition"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
