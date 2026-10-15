package vn.aicf.mobile.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import vn.aicf.mobile.domain.Role;
import vn.aicf.mobile.domain.User;

import java.time.LocalDateTime;

/** DTO cho nhóm API người dùng /api/users. */
public final class UserDtos {

    private UserDtos() {
    }

    /** Thông tin người dùng trả về cho app (KHÔNG có mật khẩu). */
    public record UserResponse(Long id, String username, String fullName, String email, String phone,
                               String avatarUrl, Role role, boolean active, boolean hasPassword,
                               LocalDateTime createdAt) {

        public static UserResponse of(User u) {
            return new UserResponse(u.getId(), u.getUsername(), u.getFullName(), u.getEmail(), u.getPhone(),
                    u.getAvatarUrl(), u.getRole(), u.isActive(), u.getPasswordHash() != null, u.getCreatedAt());
        }
    }

    /** PUT /api/users/me - người dùng tự sửa hồ sơ. */
    public record UpdateProfileRequest(
            @NotBlank(message = "không được để trống") @Size(max = 100) String fullName,
            @Email(message = "không đúng định dạng email") String email,
            @Size(max = 20) String phone) {
    }

    /** PUT /api/users/{id} - Admin đổi vai trò / khóa tài khoản. */
    public record AdminUpdateRequest(@NotNull(message = "chọn vai trò") Role role, boolean active) {
    }
}
