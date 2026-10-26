package vn.hcmute.rebook.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {
    private SecurityUtils() {}

    /** Người dùng đang đăng nhập, hoặc null nếu là khách */
    public static CustomUserDetails currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails u) return u;
        return null;
    }

    public static Long currentUserId() {
        CustomUserDetails u = currentUser();
        return u == null ? null : u.getId();
    }
}
