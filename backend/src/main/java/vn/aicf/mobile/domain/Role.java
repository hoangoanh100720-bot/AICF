package vn.aicf.mobile.domain;

/**
 * Vai trò người dùng (cột users.role).
 * <ul>
 *   <li>ADMIN    - chủ quán: toàn quyền, quản lý thực đơn / bàn / người dùng, xem thống kê, cấu hình AI</li>
 *   <li>STAFF    - nhân viên: xem và đổi trạng thái đơn hàng</li>
 *   <li>CUSTOMER - khách hàng: xem thực đơn, đặt món, theo dõi đơn của mình</li>
 * </ul>
 * Spring Security dùng dạng "ROLE_ADMIN"... (xem JwtAuthFilter).
 */
public enum Role {
    ADMIN, STAFF, CUSTOMER
}
