package vn.hcmute.rebook.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@ControllerAdvice(basePackages = "vn.hcmute.rebook.controller.web")
@lombok.RequiredArgsConstructor
public class GlobalExceptionHandler {
    private final vn.hcmute.rebook.controller.web.GlobalModelAdvice modelAdvice;

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound(NotFoundException e, Model model, HttpServletRequest request) {
        modelAdvice.addCommon(model, request);
        model.addAttribute("message", e.getMessage());
        return "error/404";
    }

    /** Lỗi nghiệp vụ: quay lại trang trước và hiện thông báo */
    @ExceptionHandler(BusinessException.class)
    public String business(BusinessException e, HttpServletRequest request, RedirectAttributes ra) {
        ra.addFlashAttribute("error", e.getMessage());
        return "redirect:" + backUrl(request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String tooLarge(HttpServletRequest request, RedirectAttributes ra) {
        ra.addFlashAttribute("error", "File quá lớn (tối đa 20MB)");
        return "redirect:" + backUrl(request);
    }

    private String backUrl(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        return referer != null ? referer : "/";
    }
}
