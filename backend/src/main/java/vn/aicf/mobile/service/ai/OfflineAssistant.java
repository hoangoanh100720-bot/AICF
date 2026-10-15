package vn.aicf.mobile.service.ai;

import org.springframework.stereotype.Component;
import vn.aicf.mobile.config.AuthUser;
import vn.aicf.mobile.service.OrderService;
import vn.aicf.mobile.service.ProductService;
import vn.aicf.mobile.service.SettingsService;
import vn.aicf.mobile.service.StatsService;
import vn.aicf.mobile.web.dto.CatalogDtos.ProductResponse;
import vn.aicf.mobile.web.dto.OrderDtos.OrderResponse;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * "CHẾ ĐỘ CƠ BẢN" của Chat AI: trả lời theo từ khóa, KHÔNG cần mạng hay khóa API.
 * Dùng khi Admin chưa nhập khóa AI, hoặc dịch vụ AI thật đang lỗi (app vẫn luôn có câu trả lời).
 * Ý tưởng lấy từ "AssistantBrain" của dự án AICF, rút gọn cho đồ án.
 */
@Component
public class OfflineAssistant {

    private final ProductService products;
    private final OrderService orders;
    private final StatsService stats;
    private final SettingsService settings;

    public OfflineAssistant(ProductService products, OrderService orders, StatsService stats, SettingsService settings) {
        this.products = products;
        this.orders = orders;
        this.stats = stats;
        this.settings = settings;
    }

    /** Trả lời 1 câu hỏi dựa trên từ khóa (đã bỏ dấu tiếng Việt để so khớp dễ hơn). */
    public String answer(AuthUser user, String message) {
        String q = normalize(message);
        if (user.isStaff() && containsAny(q, "doanh thu", "ban duoc", "thu duoc")) {
            return revenue(containsAny(q, "hom qua") ? LocalDate.now().minusDays(1) : LocalDate.now());
        }
        if (user.isStaff() && containsAny(q, "ban chay", "nhieu nhat", "top")) {
            return bestSellers();
        }
        if (user.isStaff() && containsAny(q, "dang cho", "dang xu ly", "don moi", "ban khong")) {
            List<OrderResponse> active = orders.activeOrders();
            return active.isEmpty() ? "Hiện không có đơn nào đang chờ xử lý."
                    : "Đang có " + active.size() + " đơn chờ xử lý, cũ nhất là " + active.get(0).code() + " (" + active.get(0).statusLabel() + ").";
        }
        if (containsAny(q, "don cua toi", "don hang", "trang thai", "don cua minh", "toi da dat")) {
            return myOrders(user);
        }
        if (containsAny(q, "goi y", "nen uong", "uong gi", "mon nao ngon", "de xuat")) {
            return suggest();
        }
        if (containsAny(q, "dia chi", "o dau", "gio mo", "mo cua", "dong cua")) {
            return "📍 " + settings.get(SettingsService.SHOP_NAME, "AICF Coffee") + ": "
                    + settings.get(SettingsService.SHOP_ADDRESS, "(chưa cập nhật địa chỉ)")
                    + "\n🕒 Giờ mở cửa: " + settings.get(SettingsService.SHOP_HOURS, "(chưa cập nhật)");
        }
        if (containsAny(q, "menu", "thuc don", "co mon", "gia", "bao nhieu tien", "mon gi")) {
            return menu();
        }
        return help(user);
    }

    /** Câu hỏi gợi ý hiện dưới khung chat (khác nhau theo vai trò). */
    public List<String> suggestions(AuthUser user) {
        if (user.isStaff()) {
            return List.of("Doanh thu hôm nay?", "Món nào bán chạy nhất?", "Có đơn nào đang chờ không?", "Thực đơn hiện tại");
        }
        return List.of("Gợi ý cho mình 1 món", "Thực đơn có gì?", "Đơn của tôi tới đâu rồi?", "Quán mở cửa mấy giờ?");
    }

    // ------------------------------------------------------------------

    private String menu() {
        Map<String, List<String>> byCategory = new LinkedHashMap<>();
        for (ProductResponse p : products.findAll()) {
            if (p.available()) {
                byCategory.computeIfAbsent(p.categoryName(), k -> new ArrayList<>()).add("• " + p.name() + " - " + money(p.price()));
            }
        }
        if (byCategory.isEmpty()) {
            return "Thực đơn đang được cập nhật, bạn quay lại sau nhé.";
        }
        StringBuilder sb = new StringBuilder("Thực đơn hôm nay:\n");
        byCategory.forEach((cat, list) -> sb.append("\n").append(cat).append(":\n").append(String.join("\n", list)).append("\n"));
        return sb.toString().trim();
    }

    private String suggest() {
        List<ProductResponse> available = new ArrayList<>(products.findAll().stream().filter(ProductResponse::available).toList());
        if (available.isEmpty()) {
            return "Hiện quán đang hết món, bạn quay lại sau nhé.";
        }
        Collections.shuffle(available);
        StringBuilder sb = new StringBuilder("Mình gợi ý bạn thử:\n");
        available.stream().limit(3).forEach(p -> sb.append("• ").append(p.name()).append(" (").append(money(p.price())).append(")")
                .append(p.description() == null ? "" : " - " + p.description()).append("\n"));
        return sb.toString().trim();
    }

    private String myOrders(AuthUser user) {
        List<OrderResponse> mine = orders.myOrders(user);
        if (mine.isEmpty()) {
            return "Bạn chưa có đơn nào. Vào mục Thực đơn để đặt món nhé!";
        }
        StringBuilder sb = new StringBuilder("Các đơn gần đây của bạn:\n");
        mine.stream().limit(3).forEach(o -> sb.append("• ").append(o.code()).append(": ").append(o.statusLabel())
                .append(", ").append(money(o.totalAmount())).append("\n"));
        return sb.toString().trim();
    }

    private String revenue(LocalDate day) {
        StatsService.Summary s = stats.summary(day, day, 3);
        return "Ngày " + day + ": doanh thu " + money(s.revenue()) + " từ " + s.completedCount() + " đơn hoàn thành"
                + " (tổng " + s.orderCount() + " đơn, hủy " + s.cancelledCount() + ").";
    }

    private String bestSellers() {
        LocalDate today = LocalDate.now();
        StatsService.Summary s = stats.summary(today.minusDays(6), today, 5);
        if (s.topProducts().isEmpty()) {
            return "7 ngày qua chưa có đơn hoàn thành nào để thống kê.";
        }
        StringBuilder sb = new StringBuilder("Món bán chạy 7 ngày qua:\n");
        s.topProducts().forEach(t -> sb.append("• ").append(t.productName()).append(": ").append(t.quantity())
                .append(" ly, ").append(money(t.revenue())).append("\n"));
        return sb.toString().trim();
    }

    private String help(AuthUser user) {
        return "Mình đang chạy ở chế độ cơ bản nên chỉ hiểu một số câu hỏi. Bạn thử hỏi: "
                + String.join(" / ", suggestions(user)) + ".";
    }

    /** Bỏ dấu tiếng Việt, chữ thường: "Doanh thu HÔM NAY" -> "doanh thu hom nay". */
    static String normalize(String s) {
        String noMarks = Normalizer.normalize(s.toLowerCase(Locale.ROOT), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return noMarks.replace('đ', 'd');
    }

    private static boolean containsAny(String text, String... keys) {
        for (String k : keys) {
            if (text.contains(k)) {
                return true;
            }
        }
        return false;
    }

    private static String money(long v) {
        return String.format("%,d", v).replace(',', '.') + " đ";
    }
}
