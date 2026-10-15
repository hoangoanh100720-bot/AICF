package vn.aicf.mobile.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import vn.aicf.mobile.common.ApiException;
import vn.aicf.mobile.config.AuthUser;

import java.io.IOException;
import java.util.List;

/**
 * PHẦN DÙNG CHUNG của mọi nhà cung cấp AI (mẫu <b>Template Method</b>, lấy lại từ dự án AICF):
 * chạy công cụ và đóng gói kết quả / lỗi, đọc JSON Schema của công cụ, cắt lịch sử chat.
 * Lớp con ({@link GeminiProvider}, {@link ClaudeProvider}) chỉ viết phần khác nhau: định dạng yêu cầu / phản hồi
 * và vòng lặp "AI gọi công cụ -> gửi kết quả -> AI trả lời".
 */
@Slf4j
public abstract class AbstractLlmProvider implements LlmProvider {

    /** AI được gọi công cụ tối đa bấy nhiêu vòng cho 1 câu hỏi (chống lặp vô hạn, tốn tiền). */
    protected static final int MAX_ROUNDS = 6;
    /** Kết quả công cụ dài quá thì cắt bớt. */
    protected static final int MAX_TOOL_CHARS = 12_000;
    /** Số lượt nói gần nhất gửi kèm làm ngữ cảnh. */
    protected static final int HISTORY_TURNS = 10;
    protected static final String TOO_MANY_ROUNDS = "AI hỏi dữ liệu quá nhiều lần mà chưa trả lời được, hãy hỏi cụ thể hơn";

    protected final AiToolRegistry tools;
    protected final AiSettings settings;
    protected final ObjectMapper json;

    protected AbstractLlmProvider(AiToolRegistry tools, AiSettings settings, ObjectMapper json) {
        this.tools = tools;
        this.settings = settings;
        this.json = json;
    }

    /** Kết quả chạy 1 công cụ: result != null khi thành công, ngược lại error là lý do (gửi lại cho AI). */
    protected record ToolOutcome(String result, String error) {
        boolean failed() {
            return result == null;
        }
    }

    /**
     * Chạy công cụ AI yêu cầu. Lỗi (sai tham số, không đủ quyền) KHÔNG làm hỏng cả câu trả lời:
     * lỗi được gửi lại cho AI để AI tự giải thích với người dùng.
     */
    protected ToolOutcome runTool(String name, JsonNode input, AuthUser user) {
        try {
            String out = json.writeValueAsString(tools.call(name, input, user));
            if (out.length() > MAX_TOOL_CHARS) {
                out = out.substring(0, MAX_TOOL_CHARS) + "…(đã cắt bớt)";
            }
            return new ToolOutcome(out, null);
        } catch (ApiException e) {
            return new ToolOutcome(null, e.getMessage());
        } catch (Exception e) {
            log.warn("Công cụ AI {} bị lỗi: {}", name, e.toString());
            return new ToolOutcome(null, "không lấy được dữ liệu (lỗi hệ thống)");
        }
    }

    /** JSON Schema tham số của công cụ; công cụ không tham số vẫn phải là object rỗng theo yêu cầu của các dịch vụ AI. */
    protected JsonNode schemaOf(AiTool t) throws IOException {
        return json.readTree(t.schema().equals("{}") ? "{\"type\":\"object\",\"properties\":{}}" : t.schema());
    }

    /** Chỉ giữ {@link #HISTORY_TURNS} lượt cuối, và bỏ các lượt đầu nếu không bắt đầu bằng người dùng. */
    protected static List<ChatTurn> recent(List<ChatTurn> history) {
        List<ChatTurn> tail = history.subList(Math.max(0, history.size() - HISTORY_TURNS), history.size());
        int start = 0;
        while (start < tail.size() && !tail.get(start).fromUser()) {
            start++;
        }
        return tail.subList(start, tail.size());
    }
}
