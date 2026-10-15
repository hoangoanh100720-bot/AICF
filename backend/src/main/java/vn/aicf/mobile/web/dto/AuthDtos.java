package vn.aicf.mobile.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * DTO (Data Transfer Object) cho nhóm API xác thực /api/auth.
 * DTO tách biệt với Entity: app chỉ thấy đúng các trường cần thiết, không lộ password_hash;
 * các chú thích @NotBlank, @Size... được kiểm tra tự động khi controller dùng @Valid.
 */
public final class AuthDtos {

    private AuthDtos() {
    }

    /** POST /api/auth/register - khách tự đăng ký. */
    public record RegisterRequest(
            @NotBlank(message = "không được để trống")
            @Size(min = 3, max = 40, message = "dài 3-40 ký tự")
            @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "chỉ gồm chữ không dấu, số, dấu . _ -")
            String username,

            @NotBlank(message = "không được để trống")
            @Size(min = 6, max = 100, message = "dài ít nhất 6 ký tự")
            String password,

            @NotBlank(message = "không được để trống")
            @Size(max = 100)
            String fullName,

            @Email(message = "không đúng định dạng email")
            String email,

            @Size(max = 20)
            String phone) {
    }

    /** POST /api/auth/login */
    public record LoginRequest(
            @NotBlank(message = "không được để trống") String username,
            @NotBlank(message = "không được để trống") String password) {
    }

    /** POST /api/auth/firebase - app đăng nhập Google qua Firebase rồi gửi ID token lên đây. */
    public record FirebaseLoginRequest(@NotBlank(message = "thiếu idToken") String idToken) {
    }

    /** Kết quả đăng nhập: token JWT + thông tin người dùng. */
    public record AuthResponse(String token, Instant expiresAt, UserDtos.UserResponse user) {
    }

    /** PUT /api/users/me/password */
    public record ChangePasswordRequest(
            String currentPassword,
            @NotBlank(message = "không được để trống")
            @Size(min = 6, max = 100, message = "dài ít nhất 6 ký tự")
            String newPassword) {
    }
}
