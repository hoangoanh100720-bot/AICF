package vn.aicf.mobile.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.aicf.mobile.config.AuthUser;
import vn.aicf.mobile.service.UserService;
import vn.aicf.mobile.web.dto.AuthDtos.ChangePasswordRequest;
import vn.aicf.mobile.web.dto.UserDtos.AdminUpdateRequest;
import vn.aicf.mobile.web.dto.UserDtos.UpdateProfileRequest;
import vn.aicf.mobile.web.dto.UserDtos.UserResponse;

import java.util.List;

/**
 * API NGƯỜI DÙNG.
 * <pre>
 *   GET  /api/users/me            hồ sơ của tôi                  (đã đăng nhập)
 *   PUT  /api/users/me            sửa hồ sơ                      (đã đăng nhập)
 *   PUT  /api/users/me/password   đổi mật khẩu                   (đã đăng nhập)
 *   GET  /api/users               danh sách tài khoản            (ADMIN)
 *   PUT  /api/users/{id}          đổi vai trò / khóa tài khoản   (ADMIN)
 * </pre>
 * {@code @AuthenticationPrincipal AuthUser me}: Spring đưa vào người dùng đang đăng nhập (do JwtAuthFilter xác định).
 */
@Tag(name = "02. Người dùng")
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService users;

    public UserController(UserService users) {
        this.users = users;
    }

    @GetMapping("/me")
    @Operation(summary = "Hồ sơ người đang đăng nhập")
    public UserResponse me(@AuthenticationPrincipal AuthUser me) {
        return users.me(me);
    }

    @PutMapping("/me")
    @Operation(summary = "Sửa hồ sơ (họ tên, email, điện thoại)")
    public UserResponse updateMe(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody UpdateProfileRequest request) {
        return users.updateProfile(me, request);
    }

    @PutMapping("/me/password")
    @Operation(summary = "Đổi mật khẩu")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody ChangePasswordRequest request) {
        users.changePassword(me, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "Danh sách tài khoản (ADMIN)")
    public List<UserResponse> list() {
        return users.list();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Đổi vai trò / khóa - mở khóa tài khoản (ADMIN)")
    public UserResponse adminUpdate(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                                    @Valid @RequestBody AdminUpdateRequest request) {
        return users.adminUpdate(me, id, request);
    }
}
