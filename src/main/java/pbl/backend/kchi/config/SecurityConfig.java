package pbl.backend.kchi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import pbl.backend.kchi.helper.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import java.util.Arrays;


@RequiredArgsConstructor
@Configuration
@EnableMethodSecurity(prePostEnabled=true)
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();


        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:8088",
                "http://localhost:3000",
                "*"
        ));

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Cache-Control", "Content-Type"));

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // Áp dụng cho TẤT CẢ các đường dẫn
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(@NonNull HttpSecurity http) throws  Exception{
        http
                // 1. CHẮN CHẮN TẮT CSRF VÌ DÙNG REST API/STATELESS
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth
                        // 2. CẤP QUYỀN TRUY CẬP CÔNG KHAI (PERMIT ALL) CHO TẤT CẢ TÀI NĨÊN TĨNH VÀ TRANG CHỦ
                        .requestMatchers(
                                // ⭐ TRANG CHỦ
                                "/",
                                "/index.html",
                                "/vite.svg",
                                "/assets/**", // JS, CSS, fonts

                                // API Auth
                                "/api/v1/auth/login",
                                "/api/v1/users",
                                "/api/v1/auth/refresh",

                                // Swagger/API Docs
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/api-docs",
                                "/api-docs/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/swagger-resources",
                                "/swagger-resources/**",
                                "/webjars/**",
                                "/api-docs/swagger-config"

                        ).permitAll()

                        // 3. MỌI REQUEST KHÁC ĐỀU YÊU CẦU XÁC THỰC
                        .anyRequest().authenticated()
                )

                // 4. CẤU HÌNH STATELESS VÀ THÊM FILTER JWT
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}