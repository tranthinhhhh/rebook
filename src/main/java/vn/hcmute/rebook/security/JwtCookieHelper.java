package vn.hcmute.rebook.security;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class JwtCookieHelper {
    private final JwtService jwtService;

    @Value("${app.jwt.cookie-name}")
    private String cookieName;

    public void writeToken(HttpServletResponse response, CustomUserDetails user) {
        ResponseCookie cookie = ResponseCookie.from(cookieName, jwtService.generateToken(user))
                .httpOnly(true).path("/").sameSite("Lax")
                .maxAge(Duration.ofMillis(jwtService.getExpirationMs()))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void clear(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(cookieName, "").httpOnly(true).path("/").maxAge(0).build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public String getCookieName() {
        return cookieName;
    }
}
