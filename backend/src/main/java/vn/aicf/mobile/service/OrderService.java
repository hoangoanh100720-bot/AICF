package vn.aicf.mobile.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.aicf.mobile.common.ApiException;
import vn.aicf.mobile.config.AuthUser;
import vn.aicf.mobile.domain.CustomerOrder;
import vn.aicf.mobile.domain.DiningTable;
import vn.aicf.mobile.domain.OrderItem;
import vn.aicf.mobile.domain.OrderStatus;
import vn.aicf.mobile.domain.OrderType;
import vn.aicf.mobile.domain.PaymentMethod;
import vn.aicf.mobile.domain.Product;
import vn.aicf.mobile.repository.DiningTableRepository;
import vn.aicf.mobile.repository.OrderRepository;
import vn.aicf.mobile.repository.ProductRepository;
import vn.aicf.mobile.repository.UserRepository;
import vn.aicf.mobile.web.dto.OrderDtos.CreateOrderRequest;
import vn.aicf.mobile.web.dto.OrderDtos.ItemRequest;
import vn.aicf.mobile.web.dto.OrderDtos.OrderResponse;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * NGHIỆP VỤ ĐƠN HÀNG - phần "lõi" của đồ án.
 *
 * <p>Các nguyên tắc chính:
 * <ul>
 *   <li>Giá lấy từ CSDL tại thời điểm đặt, tổng tiền do backend tính ({@link CustomerOrder#recalculateTotal()}).</li>
 *   <li>Chuyển trạng thái phải theo luật trong {@link OrderStatus#canMoveTo}.</li>
 *   <li>Khách chỉ xem / hủy đơn của chính mình; nhân viên xem và xử lý mọi đơn.</li>
 *   <li>Mỗi thay đổi quan trọng đều gửi thông báo (trong app + FCM) qua {@link NotificationService}.</li>
 * </ul>
 */
@Service
public class OrderService {

    private static final DateTimeFormatter CODE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** Các trạng thái "đang xử lý" hiển thị trên màn hình nhân viên. */
    public static final EnumSet<OrderStatus> ACTIVE =
            EnumSet.of(OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.PREPARING, OrderStatus.READY);

    private final OrderRepository orders;
    private final ProductRepository products;
    private final DiningTableRepository tables;
    private final UserRepository users;
    private final NotificationService notifications;

    public OrderService(OrderRepository orders, ProductRepository products, DiningTableRepository tables,
                        UserRepository users, NotificationService notifications) {
        this.orders = orders;
        this.products = products;
        this.tables = tables;
        this.users = users;
        this.notifications = notifications;
    }

    // ============================== KHÁCH HÀNG ==============================

    /**
     * Đặt đơn mới. Toàn bộ chạy trong 1 giao dịch: lỗi ở bất kỳ món nào thì không lưu gì cả (tính nguyên tử).
     */
    @Transactional
    public OrderResponse create(AuthUser me, CreateOrderRequest r) {
        CustomerOrder o = new CustomerOrder();
        o.setUser(users.getReferenceById(me.id()));
        o.setOrderType(r.orderType());
        o.setPaymentMethod(r.paymentMethod() == null ? PaymentMethod.CASH : r.paymentMethod());
        o.setNote(blankToNull(r.note()));

        // Đơn tại chỗ phải chọn bàn đang hoạt động; đơn mang đi thì bỏ qua bàn
        if (r.orderType() == OrderType.DINE_IN) {
            if (r.tableId() == null) {
                throw ApiException.badRequest("Đơn tại chỗ cần chọn bàn");
            }
            DiningTable t = tables.findById(r.tableId())
                    .filter(DiningTable::isActive)
                    .orElseThrow(() -> ApiException.badRequest("Bàn đã chọn không tồn tại hoặc đang tạm ngưng"));
            o.setTable(t);
        }

        for (ItemRequest ir : r.items()) {
            Product p = products.findById(ir.productId())
                    .orElseThrow(() -> ApiException.badRequest("Món #" + ir.productId() + " không còn trong thực đơn"));
            if (!p.isAvailable()) {
                throw ApiException.conflict("Món \"" + p.getName() + "\" đang tạm hết, vui lòng bỏ khỏi giỏ hàng");
            }
            OrderItem item = new OrderItem();
            item.setProduct(p);
            item.setProductName(p.getName());   // chụp lại tên + giá tại thời điểm đặt
            item.setUnitPrice(p.getPrice());
            item.setQuantity(ir.quantity());
            item.setNote(blankToNull(ir.note()));
            o.addItem(item);
        }
        o.recalculateTotal();

        // Mã đơn dựa trên id (luôn duy nhất, không bị trùng khi 2 người đặt cùng lúc):
        // lưu trước với mã tạm để SQLite cấp id, sau đó đổi thành mã đẹp DH<ngày>-<id>.
        o.setCode("TMP-" + UUID.randomUUID());
        orders.saveAndFlush(o);
        o.setCode("DH" + LocalDate.now().format(CODE_DATE) + "-" + String.format("%04d", o.getId()));

        notifications.notifyStaff("Đơn mới " + o.getCode(),
                me.fullName() + " vừa đặt " + o.getItems().size() + " món, tổng " + money(o.getTotalAmount())
                        + (o.getTable() != null ? " - " + o.getTable().getName() : " - mang đi"),
                o.getId());
        return OrderResponse.of(o);
    }

    /** Đơn của tôi (mới nhất trước). */
    @Transactional(readOnly = true)
    public List<OrderResponse> myOrders(AuthUser me) {
        return orders.findByUserIdOrderByIdDesc(me.id()).stream().map(OrderResponse::of).toList();
    }

    /** Xem 1 đơn: khách chỉ xem được đơn của mình, nhân viên xem được mọi đơn. */
    @Transactional(readOnly = true)
    public OrderResponse get(AuthUser me, Long id) {
        return OrderResponse.of(findVisible(me, id));
    }

    /** Khách tự hủy: chỉ khi quán CHƯA xác nhận đơn. */
    @Transactional
    public OrderResponse cancelByCustomer(AuthUser me, Long id, String reason) {
        CustomerOrder o = findVisible(me, id);
        if (!o.getUser().getId().equals(me.id())) {
            throw ApiException.forbidden("Bạn chỉ hủy được đơn của chính mình");
        }
        if (o.getStatus() != OrderStatus.PENDING) {
            throw ApiException.conflict("Quán đã nhận đơn, vui lòng liên hệ nhân viên để hủy");
        }
        o.setStatus(OrderStatus.CANCELLED);
        o.setCancelReason(reason == null || reason.isBlank() ? "Khách tự hủy" : reason.trim());
        notifications.notifyStaff("Đơn " + o.getCode() + " đã bị khách hủy", o.getCancelReason(), o.getId());
        return OrderResponse.of(o);
    }

    // ============================== NHÂN VIÊN ==============================

    /** Đơn đang xử lý (chưa hoàn thành / chưa hủy), đơn đến trước đứng trước. */
    @Transactional(readOnly = true)
    public List<OrderResponse> activeOrders() {
        return orders.findByStatusInOrderByIdAsc(ACTIVE).stream().map(OrderResponse::of).toList();
    }

    /** Mọi đơn trong 1 ngày (lịch sử). */
    @Transactional(readOnly = true)
    public List<OrderResponse> ordersOfDay(LocalDate day) {
        return orders.findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByIdDesc(day.atStartOfDay(), day.plusDays(1).atStartOfDay())
                .stream().map(OrderResponse::of).toList();
    }

    /** Đổi trạng thái theo đúng luật; báo cho khách biết. Hoàn thành thì tự đánh dấu đã thanh toán. */
    @Transactional
    public OrderResponse updateStatus(Long id, OrderStatus target, String cancelReason) {
        CustomerOrder o = find(id);
        OrderStatus current = o.getStatus();
        if (!current.canMoveTo(target)) {
            throw ApiException.conflict("Không thể chuyển đơn từ \"" + current.label() + "\" sang \"" + target.label() + "\"");
        }
        o.setStatus(target);
        if (target == OrderStatus.CANCELLED) {
            o.setCancelReason(cancelReason == null || cancelReason.isBlank() ? "Quán hủy đơn" : cancelReason.trim());
        }
        if (target == OrderStatus.COMPLETED) {
            o.setPaid(true);
        }
        notifications.notifyUsers(List.of(o.getUser().getId()),
                "Đơn " + o.getCode() + ": " + target.label(), customerMessage(o), o.getId());
        return OrderResponse.of(o);
    }

    /** Ghi nhận đã thanh toán (vd. khách chuyển khoản trước khi lấy đồ). */
    @Transactional
    public OrderResponse markPaid(Long id) {
        CustomerOrder o = find(id);
        if (o.getStatus() == OrderStatus.CANCELLED) {
            throw ApiException.conflict("Đơn đã hủy, không thể ghi nhận thanh toán");
        }
        o.setPaid(true);
        return OrderResponse.of(o);
    }

    // ============================== Hàm phụ ==============================

    private CustomerOrder find(Long id) {
        return orders.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy đơn #" + id));
    }

    /** Tìm đơn và kiểm tra quyền xem. Trả 404 (không phải 403) để không tiết lộ đơn của người khác có tồn tại. */
    private CustomerOrder findVisible(AuthUser me, Long id) {
        CustomerOrder o = find(id);
        if (!me.isStaff() && !o.getUser().getId().equals(me.id())) {
            throw ApiException.notFound("Không tìm thấy đơn #" + id);
        }
        return o;
    }

    private static String customerMessage(CustomerOrder o) {
        return switch (o.getStatus()) {
            case CONFIRMED -> "Quán đã nhận đơn của bạn.";
            case PREPARING -> "Đồ uống của bạn đang được pha chế.";
            case READY -> o.getTable() != null ? "Đồ uống đã xong, nhân viên sẽ mang ra " + o.getTable().getName() + "."
                    : "Đồ uống đã xong, mời bạn tới quầy nhận.";
            case COMPLETED -> "Cảm ơn bạn! Hẹn gặp lại.";
            case CANCELLED -> "Đơn đã bị hủy: " + o.getCancelReason();
            default -> "Trạng thái đơn đã thay đổi.";
        };
    }

    static String money(long amount) {
        return String.format("%,d", amount).replace(',', '.') + " đ";
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
