package vn.hcmute.rebook.controller.web;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.hcmute.rebook.dto.RegisterForm;
import vn.hcmute.rebook.dto.ResetPasswordForm;
import vn.hcmute.rebook.exception.BusinessException;
import vn.hcmute.rebook.security.CustomUserDetails;
import vn.hcmute.rebook.security.JwtCookieHelper;
import vn.hcmute.rebook.security.SecurityUtils;
import vn.hcmute.rebook.service.AuthService;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final JwtCookieHelper jwtCookieHelper;

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String redirect, Model model) {
        if (SecurityUtils.currentUser() != null) return "redirect:/";
        model.addAttribute("redirect", redirect);
        return "auth/login";
    }

    /** Đăng nhập: xác thực bằng Spring Security rồi cấp JWT lưu trong cookie HttpOnly */
    @PostMapping("/login")
    public String login(@RequestParam String email, @RequestParam String password,
                        @RequestParam(required = false) String redirect,
                        HttpServletResponse response, Model model) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email.trim().toLowerCase(), password));
            CustomUserDetails user = (CustomUserDetails) auth.getPrincipal();
            jwtCookieHelper.writeToken(response, user);
            if (redirect != null && redirect.startsWith("/") && !redirect.startsWith("//")) {
                return "redirect:" + redirect;
            }
            return switch (user.getRole()) {
                case ADMIN, MANAGER -> "redirect:/admin";
                case SHIPPER -> "redirect:/shipper";
                default -> "redirect:/";
            };
        } catch (DisabledException e) {
            model.addAttribute("error", "Tài khoản chưa được kích hoạt.");
            model.addAttribute("unverifiedEmail", email.trim().toLowerCase());
        } catch (LockedException e) {
            model.addAttribute("error", "Tài khoản đã bị khóa. Vui lòng liên hệ quản trị viên.");
        } catch (AuthenticationException e) {
            model.addAttribute("error", "Email hoặc mật khẩu không đúng");
        }
        model.addAttribute("email", email);
        model.addAttribute("redirect", redirect);
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        if (SecurityUtils.currentUser() != null) return "redirect:/";
        model.addAttribute("form", new RegisterForm());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegisterForm form, BindingResult result,
                           Model model, RedirectAttributes ra) {
        if (result.hasErrors()) return "auth/register";
        try {
            authService.register(form);
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            return "auth/register";
        }
        ra.addFlashAttribute("success", "Đã gửi mã OTP tới " + form.getEmail());
        ra.addAttribute("email", form.getEmail().trim().toLowerCase());
        return "redirect:/auth/verify";
    }

    @GetMapping("/verify")
    public String verifyPage(@RequestParam String email, Model model) {
        model.addAttribute("email", email);
        return "auth/verify";
    }

    @PostMapping("/verify")
    public String verify(@RequestParam String email, @RequestParam String otp, Model model, RedirectAttributes ra) {
        try {
            authService.activate(email, otp);
        } catch (BusinessException e) {
            model.addAttribute("email", email);
            model.addAttribute("error", e.getMessage());
            return "auth/verify";
        }
        ra.addFlashAttribute("success", "Kích hoạt tài khoản thành công! Mời bạn đăng nhập.");
        return "redirect:/auth/login";
    }

    @PostMapping("/resend")
    public String resend(@RequestParam String email, RedirectAttributes ra) {
        try {
            authService.resendRegisterOtp(email);
            ra.addFlashAttribute("success", "Đã gửi lại mã OTP");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        ra.addAttribute("email", email);
        return "redirect:/auth/verify";
    }

    @GetMapping("/forgot")
    public String forgotPage() {
        return "auth/forgot";
    }

    @PostMapping("/forgot")
    public String forgot(@RequestParam String email, Model model, RedirectAttributes ra) {
        try {
            authService.sendResetOtp(email);
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("email", email);
            return "auth/forgot";
        }
        ra.addFlashAttribute("success", "Đã gửi mã OTP đặt lại mật khẩu tới email của bạn");
        ra.addAttribute("email", email.trim().toLowerCase());
        return "redirect:/auth/reset";
    }

    @GetMapping("/reset")
    public String resetPage(@RequestParam String email, Model model) {
        ResetPasswordForm form = new ResetPasswordForm();
        form.setEmail(email);
        model.addAttribute("form", form);
        return "auth/reset";
    }

    @PostMapping("/reset")
    public String reset(@Valid @ModelAttribute("form") ResetPasswordForm form, BindingResult result,
                        Model model, RedirectAttributes ra) {
        if (result.hasErrors()) return "auth/reset";
        try {
            authService.resetPassword(form);
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            return "auth/reset";
        }
        ra.addFlashAttribute("success", "Đặt lại mật khẩu thành công! Mời bạn đăng nhập.");
        return "redirect:/auth/login";
    }
}
