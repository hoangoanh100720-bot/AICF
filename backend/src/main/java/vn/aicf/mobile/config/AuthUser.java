package vn.aicf.mobile.config;

import vn.aicf.mobile.domain.Role;
import vn.aicf.mobile.domain.User;

/**
 * Người dùng ĐANG ĐĂNG NHẬP của request hiện tại (do {@link JwtAuthFilter} tạo từ token).
 * Controller nhận bằng {@code @AuthenticationPrincipal AuthUser me}.
 * Là record bất biến, chỉ chứa thông tin cần thiết - không giữ Entity để tránh lỗi lazy-loading ngoài giao dịch.
 */
public record AuthUser(Long id, String username, String fullName, Role role) {

    public static AuthUser of(User u) {
        return new AuthUser(u.getId(), u.getUsername(), u.getFullName(), u.getRole());
    }

    /** Nhân viên hoặc quản trị (được xử lý đơn hàng). */
    public boolean isStaff() {
        return role == Role.STAFF || role == Role.ADMIN;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
