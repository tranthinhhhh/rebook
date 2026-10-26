package vn.hcmute.rebook.util;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponents;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Dùng trong Thymeleaf: ${@fmt.money(p.finalPrice)} */
@Component("fmt")
public class Formatter {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public String money(long amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator('.');
        return new DecimalFormat("#,###", symbols).format(amount) + " ₫";
    }

    public String dateTime(LocalDateTime t) {
        return t == null ? "" : t.format(DATE_TIME);
    }

    public String date(LocalDateTime t) {
        return t == null ? "" : t.format(DATE);
    }

    public String rating(double r) {
        return String.format(Locale.US, "%.1f", r);
    }

    /** Màu nền ổn định theo id, dùng cho bìa sách tự sinh khi chưa có ảnh */
    public String coverColor(Long id) {
        String[] colors = {"#8c4a2f", "#2f5d62", "#5b4a8c", "#7a6c2f", "#2f4a8c", "#8c2f4f", "#3d6b3a", "#6b3a5d", "#a05a2c", "#34495e"};
        return colors[(int) (Math.abs(id == null ? 0 : id) % colors.length)];
    }

    /** URL trang hiện tại với tham số page mới (giữ nguyên các bộ lọc khác) */
    public String pageUrl(int page) {
        UriComponents c = ServletUriComponentsBuilder.fromCurrentRequest().replaceQueryParam("page", page).build();
        return c.getPath() + (c.getQuery() != null ? "?" + c.getQuery() : "");
    }

    /** URL trang hiện tại với một tham số được thay giá trị (về trang 0) */
    public String withParam(String name, Object value) {
        UriComponents c = ServletUriComponentsBuilder.fromCurrentRequest()
                .replaceQueryParam("page")
                .replaceQueryParam(name, value == null ? new Object[0] : new Object[]{value}).build();
        return c.getPath() + (c.getQuery() != null ? "?" + c.getQuery() : "");
    }
}
