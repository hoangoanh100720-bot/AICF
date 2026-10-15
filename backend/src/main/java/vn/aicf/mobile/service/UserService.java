package vn.aicf.mobile.service;

import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.aicf.mobile.common.ApiException;
import vn.aicf.mobile.config.AuthUser;
import vn.aicf.mobile.domain.User;
import vn.aicf.mobile.repository.UserRepository;
import vn.aicf.mobile.web.dto.AuthDtos.ChangePasswordRequest;
import vn.aicf.mobile.web.dto.UserDtos.AdminUpdateRequest;
import vn.aicf.mobile.web.dto.UserDtos.UpdateProfileRequest;
import vn.aicf.mobile.web.dto.UserDtos.UserResponse;

import java.util.List;

/** Hồ sơ cá nhân (mọi người dùng) và quản lý tài khoản (Admin). */
@Service
public class UserService {

    private final UserRepository users;
    private final PasswordEncoder encoder;

    public UserService(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @Transactional(readOnly = true)
    public UserResponse me(AuthUser me) {
        return UserResponse.of(get(me.id()));
    }

    @Transactional
    public UserResponse updateProfile(AuthUser me, UpdateProfileRequest r) {
        User u = get(me.id());
        String email = AuthService.blankToNull(r.email());
        if (email != null && !email.equalsIgnoreCase(u.getEmail()) && users.existsByEmail(email)) {
            throw ApiException.conflict("Email đã được dùng cho tài khoản khác");
        }
        u.setFullName(r.fullName().trim());
        u.setEmail(email);
        u.setPhone(AuthService.blankToNull(r.phone()));
        return UserResponse.of(u);   // trong giao dịch: JPA tự UPDATE khi kết thúc (dirty checking), không cần save()
    }

    /** Đổi mật khẩu. Tài khoản Google chưa có mật khẩu thì được đặt mật khẩu mới không cần mật khẩu cũ. */
    @Transactional
    public void changePassword(AuthUser me, ChangePasswordRequest r) {
        User u = get(me.id());
        if (u.getPasswordHash() != null
                && (r.currentPassword() == null || !encoder.matches(r.currentPassword(), u.getPasswordHash()))) {
            throw ApiException.badRequest("Mật khẩu hiện tại không đúng");
        }
        u.setPasswordHash(encoder.encode(r.newPassword()));
    }

    // ---------------- Admin ----------------

    @Transactional(readOnly = true)
    public List<UserResponse> list() {
        return users.findAll(Sort.by("role", "username")).stream().map(UserResponse::of).toList();
    }

    /** Admin đổi vai trò / khóa tài khoản. Không cho tự hạ quyền hoặc tự khóa chính mình (tránh mất quyền quản trị). */
    @Transactional
    public UserResponse adminUpdate(AuthUser me, Long id, AdminUpdateRequest r) {
        User u = get(id);
        if (u.getId().equals(me.id()) && (r.role() != u.getRole() || !r.active())) {
            throw ApiException.badRequest("Không thể tự đổi vai trò hoặc tự khóa tài khoản của chính mình");
        }
        u.setRole(r.role());
        u.setActive(r.active());
        return UserResponse.of(u);
    }

    private User get(Long id) {
        return users.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy người dùng #" + id));
    }
}
