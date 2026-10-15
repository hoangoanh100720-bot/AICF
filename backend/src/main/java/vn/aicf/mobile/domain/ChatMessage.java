package vn.aicf.mobile.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Bảng CHAT_MESSAGES - một tin nhắn trong cuộc trò chuyện với Trợ lý AI. */
@Getter
@Setter
@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    /** Vai trò của tin nhắn. */
    public static final String USER = "USER";
    public static final String ASSISTANT = "ASSISTANT";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** USER (người hỏi) hoặc ASSISTANT (AI trả lời). */
    @Column(nullable = false)
    private String role;

    @Column(nullable = false)
    private String content;

    /** Với tin của AI: "LLM" = AI thật trả lời, "OFFLINE" = chế độ cơ bản. */
    private String mode;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
