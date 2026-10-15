package vn.aicf.mobile.domain;

import java.util.Map;
import java.util.Set;

/**
 * Trạng thái đơn hàng và luật chuyển trạng thái (máy trạng thái - state machine).
 *
 * <pre>
 *   PENDING ──> CONFIRMED ──> PREPARING ──> READY ──> COMPLETED
 *      │            │              │           │
 *      └────────────┴──────────────┴───────────┴──> CANCELLED
 * </pre>
 * Đặt luật ở MỘT chỗ giúp mọi nơi (API nhân viên, khách tự hủy, AI...) đều tuân theo cùng một quy tắc.
 */
public enum OrderStatus {
    PENDING("Chờ xác nhận"),
    CONFIRMED("Đã xác nhận"),
    PREPARING("Đang pha chế"),
    READY("Đã xong, chờ lấy"),
    COMPLETED("Hoàn thành"),
    CANCELLED("Đã hủy");

    /** Tên tiếng Việt để hiển thị / gửi thông báo. */
    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** Từ mỗi trạng thái được phép đi tới những trạng thái nào. */
    private static final Map<OrderStatus, Set<OrderStatus>> NEXT = Map.of(
            PENDING, Set.of(CONFIRMED, CANCELLED),
            CONFIRMED, Set.of(PREPARING, CANCELLED),
            PREPARING, Set.of(READY, CANCELLED),
            READY, Set.of(COMPLETED, CANCELLED),
            COMPLETED, Set.of(),
            CANCELLED, Set.of());

    /** Có được chuyển từ trạng thái hiện tại sang {@code target} không. */
    public boolean canMoveTo(OrderStatus target) {
        return NEXT.get(this).contains(target);
    }

    /** Đơn đã kết thúc (không đổi được nữa). */
    public boolean isFinal() {
        return this == COMPLETED || this == CANCELLED;
    }
}
