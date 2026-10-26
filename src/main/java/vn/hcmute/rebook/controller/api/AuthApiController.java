package vn.hcmute.rebook.controller.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;
import vn.hcmute.rebook.dto.ApiLoginRequest;
import vn.hcmute.rebook.security.CustomUserDetails;
import vn.hcmute.rebook.security.JwtService;
import vn.hcmute.rebook.security.SecurityUtils;

import java.util.Map;

/** REST API xác thực: trả về JWT để gọi các API khác bằng header Authorization: Bearer <token> */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthApiController {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody ApiLoginRequest req) {
        try {
            var auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.email().trim().toLowerCase(), req.password()));
            CustomUserDetails user = (CustomUserDetails) auth.getPrincipal();
            return ResponseEntity.ok(Map.of(
                    "token", jwtService.generateToken(user),
                    "type", "Bearer",
                    "expiresInMs", jwtService.getExpirationMs(),
                    "user", Map.of("id", user.getId(), "email", user.getEmail(), "fullName", user.getFullName(), "role", user.getRole())));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(401).body(Map.of("error", "Email hoặc mật khẩu không đúng, hoặc tài khoản chưa kích hoạt / bị khóa"));
        }
    }

    @GetMapping("/me")
    public Map<String, Object> me() {
        CustomUserDetails u = SecurityUtils.currentUser();
        return Map.of("id", u.getId(), "email", u.getEmail(), "fullName", u.getFullName(), "role", u.getRole());
    }
}
