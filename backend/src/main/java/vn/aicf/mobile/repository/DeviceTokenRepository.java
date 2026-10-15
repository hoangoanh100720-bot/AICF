package vn.aicf.mobile.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.aicf.mobile.domain.DeviceToken;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Truy vấn bảng DEVICE_TOKENS. */
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    Optional<DeviceToken> findByToken(String token);

    /** Mọi mã thiết bị của một nhóm người dùng (để gửi FCM). */
    @Query("SELECT d.token FROM DeviceToken d WHERE d.user.id IN :userIds")
    List<String> findTokensOfUsers(@Param("userIds") Collection<Long> userIds);

    /** Xóa các mã mà Firebase báo đã hết hiệu lực (app bị gỡ, người dùng tắt thông báo...). */
    @Modifying
    @Query("DELETE FROM DeviceToken d WHERE d.token IN :tokens")
    int deleteTokens(@Param("tokens") Collection<String> tokens);

    /** Xóa 1 mã của chính người dùng (khi đăng xuất trên thiết bị đó). */
    @Modifying
    @Query("DELETE FROM DeviceToken d WHERE d.token = :token AND d.user.id = :userId")
    int deleteTokenOfUser(@Param("token") String token, @Param("userId") Long userId);
}
