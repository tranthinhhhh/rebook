package vn.hcmute.rebook.config;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import vn.hcmute.rebook.security.CsrfCookieFilter;
import vn.hcmute.rebook.security.CustomUserDetailsService;
import vn.hcmute.rebook.security.JwtAuthenticationFilter;
import vn.hcmute.rebook.security.JwtCookieHelper;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService userDetailsService;
    private final JwtCookieHelper jwtCookieHelper;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider::authenticate;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // JWT => không dùng session phía server
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(csrf -> csrf
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                    // JWT xác thực lại ở mỗi request: không xoay vòng CSRF token mỗi lần (tránh lệch token giữa các tab/AJAX)
                    .sessionAuthenticationStrategy(new NullAuthenticatedSessionStrategy())
                    .ignoringRequestMatchers("/api/**", "/ws/**"))
            .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/css/**", "/js/**", "/img/**", "/uploads/**", "/favicon.ico", "/error/**").permitAll()
                    .requestMatchers("/", "/search", "/books/**", "/category/**", "/stores/**", "/auth/**", "/ws/**").permitAll()
                    .requestMatchers("/api/auth/**", "/api/books/**").permitAll()
                    .requestMatchers("/admin/accounts/**", "/admin/settings/**").hasRole("ADMIN")
                    .requestMatchers("/admin/**").hasAnyRole("ADMIN", "MANAGER")
                    .requestMatchers("/vendor/register").authenticated()
                    .requestMatchers("/vendor/**").hasRole("VENDOR")
                    .requestMatchers("/shipper/**").hasRole("SHIPPER")
                    .anyRequest().authenticated())
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint((req, res, e) -> {
                        if (req.getRequestURI().startsWith("/api/")) {
                            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            res.setContentType("application/json;charset=UTF-8");
                            res.getWriter().write("{\"error\":\"Bạn cần đăng nhập\"}");
                        } else {
                            String back = req.getRequestURI() + (req.getQueryString() != null ? "?" + req.getQueryString() : "");
                            res.sendRedirect("/auth/login?redirect=" + URLEncoder.encode(back, StandardCharsets.UTF_8));
                        }
                    })
                    .accessDeniedHandler((req, res, e) -> {
                        if (req.getRequestURI().startsWith("/api/")) {
                            res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            res.setContentType("application/json;charset=UTF-8");
                            res.getWriter().write("{\"error\":\"Bạn không có quyền truy cập\"}");
                        } else {
                            res.sendRedirect("/error/403");
                        }
                    }))
            .formLogin(f -> f.disable())
            .httpBasic(b -> b.disable())
            .logout(l -> l
                    .logoutUrl("/auth/logout")
                    .addLogoutHandler((req, res, auth) -> jwtCookieHelper.clear(res))
                    .logoutSuccessUrl("/?logout"))
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
