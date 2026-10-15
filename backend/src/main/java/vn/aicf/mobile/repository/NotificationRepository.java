package vn.aicf.mobile.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.aicf.mobile.domain.Notification;

import java.util.List;

/** Truy vấn bảng NOTIFICATIONS. */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByIdDesc(Long userId, Pageable page);

    long countByUserIdAndReadFalse(Long userId);

    /** Đánh dấu đã đọc tất cả thông báo của 1 người. */
    @Modifying
    @Query("UPDATE Notification n SET n.read = TRUE WHERE n.user.id = :userId AND n.read = FALSE")
    int markAllRead(@Param("userId") Long userId);
}
