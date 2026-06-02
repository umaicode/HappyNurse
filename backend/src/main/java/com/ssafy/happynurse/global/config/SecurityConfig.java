package com.ssafy.happynurse.global.config;

import com.ssafy.happynurse.global.security.JwtAccessDeniedHandler;
import com.ssafy.happynurse.global.security.JwtAuthenticationEntryPoint;
import com.ssafy.happynurse.global.security.JwtAuthenticationFilter;
import com.ssafy.happynurse.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAccessDeniedHandler accessDeniedHandler;

    @Value("${jwt.cookie-name}")
    private String cookieName;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .exceptionHandling(eh -> eh
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/login", "/auth/refresh", "/auth/signup", "/auth/dev-login").permitAll()
                .requestMatchers("/app/auth/login", "/app/auth/refresh").permitAll()
                .requestMatchers("/swagger-ui/**", "/api-docs/**", "/v3/api-docs/**",
                    "/api/swagger-ui/**", "/api/v3/api-docs/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/actuator/health", "/actuator/prometheus").permitAll() // 모니터링: infra-net 내부 Prometheus 스크랩용 (nginx 공개 금지)
                .requestMatchers("/nfc/patients/**").permitAll()    // 환자 NFC 진입
                .requestMatchers("/patients/verify").permitAll()    // 환자 본인 확인
                .requestMatchers("/patients/dev-verify").permitAll()    // 환자 dev 버전 본인 확인
                .requestMatchers("/nfc/redirect").permitAll()       // NFC 진입 redirect
                    .requestMatchers("/his/**").permitAll()         // HIS 시뮬레이터 (API key로 별도 인증)
                    .requestMatchers("/internal/ai/**").permitAll() // AI 서버 -> 간호기록 SSE notify (nginx를 신뢰한다는 가정. 추후, API key 검증 보강 필요)
                    .requestMatchers(HttpMethod.GET, "/organizations").permitAll()           // 로그인 화면 병원 목록
                .requestMatchers(HttpMethod.GET, "/organizations/*/wards").permitAll()   // 로그인 화면 병동 목록
                .anyRequest().authenticated())
            .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider, cookieName),
                UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        // HIS 시뮬레이터 — 로컬 발사기 허용
        CorsConfiguration simConfig = new CorsConfiguration();
        simConfig.addAllowedOriginPattern("*");
        simConfig.setAllowedMethods(List.of("GET", "POST", "PATCH", "OPTIONS"));
        simConfig.setAllowedHeaders(List.of("*"));
        simConfig.setMaxAge(3600L);
        source.registerCorsConfiguration("/his/**", simConfig);

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000", "https://K14e101.p.ssafy.io"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}