package vn.aicf.mobile.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import vn.aicf.mobile.domain.CustomerOrder;
import vn.aicf.mobile.domain.OrderItem;
import vn.aicf.mobile.domain.OrderStatus;
import vn.aicf.mobile.domain.OrderType;
import vn.aicf.mobile.domain.PaymentMethod;

import java.time.LocalDateTime;
import java.util.List;

/** DTO cho nhóm API đơn hàng /api/orders. */
public final class OrderDtos {

    private OrderDtos() {
    }

    /**
     * POST /api/orders - khách đặt đơn. Ví dụ JSON:
     * <pre>
     * {
     *   "orderType": "DINE_IN", "tableId": 2, "paymentMethod": "CASH", "note": "Mang ra nhanh giúp mình",
     *   "items": [ { "productId": 1, "quantity": 2, "note": "ít đá" }, { "productId": 5, "quantity": 1 } ]
     * }
     * </pre>
     * Chỉ gửi id món và số lượng; GIÁ do backend tự lấy từ CSDL (không tin giá app gửi lên).
     */
    public record CreateOrderRequest(
            @NotNull(message = "chọn tại chỗ hoặc mang đi") OrderType orderType,
            Long tableId,
            PaymentMethod paymentMethod,
            @Size(max = 500) String note,
            @NotEmpty(message = "giỏ hàng đang trống") @Size(max = 50) List<@Valid ItemRequest> items) {
    }

    public record ItemRequest(
            @NotNull(message = "thiếu id món") Long productId,
            @Min(value = 1, message = "ít nhất 1") @Max(value = 99, message = "tối đa 99") int quantity,
            @Size(max = 200) String note) {
    }

    /** PATCH /api/orders/{id}/status - nhân viên đổi trạng thái. */
    public record UpdateStatusRequest(@NotNull(message = "chọn trạng thái") OrderStatus status,
                                      @Size(max = 200) String cancelReason) {
    }

    /** POST /api/orders/{id}/cancel - khách tự hủy (chỉ khi đơn còn chờ xác nhận). */
    public record CancelRequest(@Size(max = 200) String reason) {
    }

    public record ItemResponse(Long id, Long productId, String productName, long unitPrice, int quantity,
                               long lineTotal, String note) {

        static ItemResponse of(OrderItem i) {
            return new ItemResponse(i.getId(), i.getProduct() == null ? null : i.getProduct().getId(),
                    i.getProductName(), i.getUnitPrice(), i.getQuantity(), i.getUnitPrice() * i.getQuantity(), i.getNote());
        }
    }

    public record OrderResponse(Long id, String code, Long userId, String customerName, Long tableId, String tableName,
                                OrderType orderType, OrderStatus status, String statusLabel, long totalAmount,
                                PaymentMethod paymentMethod, boolean paid, String note, String cancelReason,
                                List<ItemResponse> items, LocalDateTime createdAt, LocalDateTime updatedAt) {

        /** Đổi Entity -> DTO. Phải gọi TRONG giao dịch vì đụng tới các quan hệ LAZY (user, table, items). */
        public static OrderResponse of(CustomerOrder o) {
            return new OrderResponse(o.getId(), o.getCode(), o.getUser().getId(), o.getUser().getFullName(),
                    o.getTable() == null ? null : o.getTable().getId(),
                    o.getTable() == null ? null : o.getTable().getName(),
                    o.getOrderType(), o.getStatus(), o.getStatus().label(), o.getTotalAmount(),
                    o.getPaymentMethod(), o.isPaid(), o.getNote(), o.getCancelReason(),
                    o.getItems().stream().map(ItemResponse::of).toList(), o.getCreatedAt(), o.getUpdatedAt());
        }
    }
}
