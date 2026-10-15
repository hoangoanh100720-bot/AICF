package vn.aicf.mobile.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import vn.aicf.mobile.common.ApiException;
import vn.aicf.mobile.config.AuthUser;
import vn.aicf.mobile.service.OrderService;
import vn.aicf.mobile.service.ProductService;
import vn.aicf.mobile.service.SettingsService;
import vn.aicf.mobile.service.StatsService;
import vn.aicf.mobile.web.dto.CatalogDtos.ProductResponse;
import vn.aicf.mobile.web.dto.OrderDtos.OrderResponse;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DANH SÁCH CÔNG CỤ của Chat AI (xem {@link AiTool}). Mọi công cụ chỉ ĐỌC dữ liệu, không sửa gì.
 *
 * <pre>
 *   Tên công cụ          Ai dùng được   Trả về
 *   get_menu             mọi người      thực đơn: danh mục, tên món, giá, còn bán không
 *   get_shop_info        mọi người      tên quán, địa chỉ, giờ mở cửa
 *   get_my_orders        mọi người      các đơn gần đây của chính người hỏi
 *   get_sales_summary    nhân viên+     doanh thu, số đơn, món bán chạy trong khoảng ngày
 *   get_active_orders    nhân viên+     các đơn đang xử lý
 * </pre>
 * Thêm công cụ mới = thêm 1 phần tử vào danh sách trong constructor. Các nhà cung cấp AI (Gemini, Claude)
 * và chế độ cơ bản đều dùng chung danh sách này.
 */
@Component
public class AiToolRegistry {

    private final Map<String, AiTool> tools = new LinkedHashMap<>();

    public AiToolRegistry(ProductService products, OrderService orders, StatsService stats, SettingsService settings) {
        register(new AiTool("get_menu",
                "Lấy thực đơn của quán (tên món, danh mục, giá bằng đồng, mô tả, còn bán hay tạm hết). "
                        + "Dùng khi khách hỏi có món gì, giá bao nhiêu, nhờ gợi ý món.",
                "{}", false,
                (args, user) -> products.findAll().stream().map(AiToolRegistry::menuItem).toList()));

        register(new AiTool("get_shop_info",
                "Lấy tên quán, địa chỉ và giờ mở cửa.",
                "{}", false,
                (args, user) -> Map.of(
                        "name", settings.get(SettingsService.SHOP_NAME, "AICF Coffee"),
                        "address", settings.get(SettingsService.SHOP_ADDRESS, ""),
                        "hours", settings.get(SettingsService.SHOP_HOURS, ""))));

        register(new AiTool("get_my_orders",
                "Lấy tối đa 5 đơn hàng gần nhất CỦA NGƯỜI ĐANG HỎI (mã đơn, trạng thái, tổng tiền, các món).",
                "{}", false,
                (args, user) -> orders.myOrders(user).stream().limit(5).map(AiToolRegistry::orderBrief).toList()));

        register(new AiTool("get_sales_summary",
                "Thống kê bán hàng trong khoảng ngày: doanh thu (chỉ đơn hoàn thành), tổng số đơn, số đơn hoàn thành, "
                        + "số đơn hủy và các món bán chạy. Ngày theo định dạng YYYY-MM-DD.",
                """
                {"type":"object","properties":{
                  "date_from":{"type":"string","description":"Ngày bắt đầu, YYYY-MM-DD"},
                  "date_to":{"type":"string","description":"Ngày kết thúc (tính cả ngày này), YYYY-MM-DD"}},
                 "required":["date_from","date_to"]}
                """, true,
                (args, user) -> {
                    LocalDate from = date(args, "date_from");
                    LocalDate to = date(args, "date_to");
                    if (to.isBefore(from)) {
                        throw ApiException.badRequest("date_to phải sau date_from");
                    }
                    return stats.summary(from, to, 5);
                }));

        register(new AiTool("get_active_orders",
                "Lấy các đơn đang xử lý (chờ xác nhận, đã nhận, đang pha, chờ lấy) để biết quán đang bận cỡ nào.",
                "{}", true,
                (args, user) -> orders.activeOrders().stream().map(AiToolRegistry::orderBrief).toList()));
    }

    private void register(AiTool tool) {
        tools.put(tool.name(), tool);
    }

    /** Các công cụ người dùng này được phép dùng (khách không thấy công cụ doanh thu). */
    public List<AiTool> availableFor(AuthUser user) {
        return tools.values().stream().filter(t -> !t.staffOnly() || user.isStaff()).toList();
    }

    /** Chạy 1 công cụ theo tên. Kiểm tra quyền LẦN NỮA ở đây phòng khi AI cố gọi công cụ không được cấp. */
    public Object call(String name, JsonNode args, AuthUser user) {
        AiTool tool = tools.get(name);
        if (tool == null) {
            throw ApiException.badRequest("Không có công cụ " + name);
        }
        if (tool.staffOnly() && !user.isStaff()) {
            throw ApiException.forbidden("Bạn không có quyền xem dữ liệu này");
        }
        return tool.handler().apply(args, user);
    }

    // ---------- Rút gọn dữ liệu trước khi gửi cho AI (ít chữ hơn = nhanh hơn, rẻ hơn) ----------

    private static Map<String, Object> menuItem(ProductResponse p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("category", p.categoryName());
        m.put("name", p.name());
        m.put("price", p.price());
        m.put("description", p.description());
        m.put("available", p.available());
        return m;
    }

    private static Map<String, Object> orderBrief(OrderResponse o) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("code", o.code());
        m.put("status", o.statusLabel());
        m.put("total", o.totalAmount());
        m.put("table", o.tableName() == null ? "mang đi" : o.tableName());
        m.put("createdAt", o.createdAt().toString());
        m.put("items", o.items().stream().map(i -> i.quantity() + " x " + i.productName()).toList());
        return m;
    }

    private static LocalDate date(JsonNode args, String field) {
        String s = args == null ? "" : args.path(field).asText("");
        try {
            return LocalDate.parse(s.trim());
        } catch (DateTimeParseException e) {
            throw ApiException.badRequest(field + " phải có dạng YYYY-MM-DD");
        }
    }
}
