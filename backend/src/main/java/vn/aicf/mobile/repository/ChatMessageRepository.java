package vn.aicf.mobile.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.aicf.mobile.domain.ChatMessage;

import java.util.List;

/** Truy vấn bảng CHAT_MESSAGES. */
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /** N tin nhắn MỚI NHẤT của 1 người (Pageable giới hạn số dòng = LIMIT trong SQL). */
    List<ChatMessage> findByUserIdOrderByIdDesc(Long userId, Pageable page);

    /** Xóa toàn bộ lịch sử chat của 1 người. @Modifying: câu lệnh thay đổi dữ liệu, không phải SELECT. */
    @Modifying
    @Query("DELETE FROM ChatMessage m WHERE m.user.id = :userId")
    int deleteAllOfUser(@Param("userId") Long userId);
}
