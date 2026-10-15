package vn.aicf.mobile.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.aicf.mobile.config.AuthUser;
import vn.aicf.mobile.service.OrderService;
import vn.aicf.mobile.web.dto.OrderDtos.CancelRequest;
import vn.aicf.mobile.web.dto.OrderDtos.CreateOrderRequest;
import vn.aicf.mobile.web.dto.OrderDtos.OrderResponse;
import vn.aicf.mobile.web.dto.OrderDtos.UpdateStatusRequest;

import java.time.LocalDate;
import java.util.List;

/**
 * API ĐƠN HÀNG (đều cần đăng nhập).
 * <pre>
 *   POST  /api/orders                đặt đơn                          (khách)        201
 *   GET   /api/orders/my             đơn của tôi                      (khách)
 *   GET   /api/orders/{id}           chi tiết đơn                     (chủ đơn hoặc nhân viên)
 *   POST  /api/orders/{id}/cancel    khách tự hủy (khi chờ xác nhận)  (chủ đơn)
 *   GET   /api/orders/active         đơn đang xử lý                   (STAFF, ADMIN)
 *   GET   /api/orders?date=YYYY-MM-DD  mọi đơn trong ngày            (STAFF, ADMIN)
 *   PATCH /api/orders/{id}/status    đổi trạng thái                   (STAFF, ADMIN)
 *   PATCH /api/orders/{id}/paid      ghi nhận đã thanh toán           (STAFF, ADMIN)
 * </pre>
 * PATCH (sửa một phần) dùng cho thao tác chỉ đổi 1-2 trường của đơn, khác PUT (thay toàn bộ).
 */
@Tag(name = "6. Đơn hàng")
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orders;

    public OrderController(OrderService orders) {
        this.orders = orders;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Đặt đơn mới (giá do máy chủ tính)")
    public OrderResponse create(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody CreateOrderRequest request) {
        return orders.create(me, request);
    }

    @GetMapping("/my")
    @Operation(summary = "Đơn của tôi, mới nhất trước")
    public List<OrderResponse> my(@AuthenticationPrincipal AuthUser me) {
        return orders.myOrders(me);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết 1 đơn")
    public OrderResponse get(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return orders.get(me, id);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Khách tự hủy đơn (chỉ khi quán chưa xác nhận)")
    public OrderResponse cancel(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                                @Valid @RequestBody(required = false) CancelRequest request) {
        return orders.cancelByCustomer(me, id, request == null ? null : request.reason());
    }

    // ---------------- Nhân viên ----------------

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    @Operation(summary = "Đơn đang xử lý (STAFF/ADMIN)")
    public List<OrderResponse> active() {
        return orders.activeOrders();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    @Operation(summary = "Mọi đơn trong 1 ngày, mặc định hôm nay (STAFF/ADMIN)")
    public List<OrderResponse> ofDay(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return orders.ordersOfDay(date == null ? LocalDate.now() : date);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    @Operation(summary = "Đổi trạng thái đơn theo luật PENDING→CONFIRMED→PREPARING→READY→COMPLETED (STAFF/ADMIN)")
    public OrderResponse updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateStatusRequest request) {
        return orders.updateStatus(id, request.status(), request.cancelReason());
    }

    @PatchMapping("/{id}/paid")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    @Operation(summary = "Ghi nhận đơn đã thanh toán (STAFF/ADMIN)")
    public OrderResponse markPaid(@PathVariable Long id) {
        return orders.markPaid(id);
    }
}
