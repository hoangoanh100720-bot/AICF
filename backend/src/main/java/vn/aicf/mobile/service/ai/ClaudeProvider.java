package vn.aicf.mobile.service.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonValue;
import com.anthropic.errors.AnthropicException;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.TextBlock;
import com.anthropic.models.messages.Tool;
import com.anthropic.models.messages.ToolResultBlockParam;
import com.anthropic.models.messages.ToolUseBlock;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Anthropic CLAUDE qua SDK Java chính thức (com.anthropic:anthropic-java), API Messages + tool use.
 *
 * <p>Vòng lặp gọi công cụ (manual tool loop):
 * <ol>
 *   <li>Gửi lịch sử + câu hỏi + danh sách công cụ.</li>
 *   <li>Nếu Claude trả về khối {@code tool_use}: chạy công cụ, gửi lại khối {@code tool_result} (TẤT CẢ kết quả
 *       trong 1 tin nhắn user), quay lại bước 1.</li>
 *   <li>Nếu không còn {@code tool_use}: ghép các khối chữ thành câu trả lời.</li>
 * </ol>
 * Model mặc định: claude-opus-5-5 (đổi được trong Cài đặt AI). Model này luôn "suy nghĩ thích ứng"
 * (adaptive thinking); với chat hỏi đáp ngắn ta đặt mức nỗ lực (effort) thấp để trả lời nhanh, rẻ.
 */
@Component
public class ClaudeProvider extends AbstractLlmProvider {

    private static final long MAX_TOKENS = 16_000L;
    /** Các model hỗ trợ "server-side fallback": bị bộ lọc an toàn từ chối thì máy chủ Anthropic tự thử model dự phòng. */
    private static final Set<String> FALLBACK_MODELS = Set.of("claude-opus-5-5", "claude-opus-5", "claude-sonnet-5-5", "claude-fable-5-1");

    /** Client SDK tạo lại khi Admin đổi khóa API. */
    private AnthropicClient client;
    private String clientKey;

    public ClaudeProvider(AiToolRegistry tools, AiSettings settings, ObjectMapper json) {
        super(tools, settings, json);
    }

    @Override
    public String id() {
        return "claude";
    }

    @Override
    public boolean hasKey() {
        return !settings.claudeKey().isBlank();
    }

    @Override
    public String model() {
        return settings.claudeModel();
    }

    @Override
    public String chat(LlmRequest req) throws IOException {
        String model = model();
        MessageCreateParams.Builder params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(MAX_TOKENS)
                .system(req.systemPrompt())
                .outputConfig(OutputConfig.builder().effort(OutputConfig.Effort.LOW).build());
        if (FALLBACK_MODELS.contains(model)) {
            params.putAdditionalHeader("anthropic-beta", "server-side-fallback-2026-07-01")
                    .putAdditionalBodyProperty("fallbacks", JsonValue.from("default"));
        }
        for (AiTool t : req.tools()) {
            params.addTool(toClaudeTool(t));
        }
        // Lịch sử: gộp các lượt liên tiếp cùng vai trò để luôn xen kẽ user / assistant
        List<ChatTurn> turns = new ArrayList<>(recent(req.history()));
        turns.add(new ChatTurn("USER", req.message()));
        for (ChatTurn t : merge(turns)) {
            if (t.fromUser()) {
                params.addUserMessage(t.content());
            } else {
                params.addAssistantMessage(t.content());
            }
        }

        try {
            for (int round = 0; round < MAX_ROUNDS; round++) {
                Message resp = client().messages().create(params.build());

                // Luôn kiểm tra stop_reason trước khi đọc nội dung
                if (resp.stopReason().map(r -> r.equals(StopReason.REFUSAL)).orElse(false)) {
                    throw new IOException("Dịch vụ AI từ chối trả lời câu hỏi này");
                }
                List<ToolUseBlock> uses = resp.content().stream().flatMap(b -> b.toolUse().stream()).toList();
                if (uses.isEmpty()) {
                    String out = String.join("", resp.content().stream()
                            .flatMap(b -> b.text().stream()).map(TextBlock::text).toList()).trim();
                    if (out.isEmpty()) {
                        throw new IOException("Dịch vụ AI không trả về nội dung");
                    }
                    return out;
                }
                // Gửi lại NGUYÊN VẸN lượt trả lời của Claude (kể cả khối thinking) rồi tới kết quả công cụ
                params.addMessage(resp);
                List<ContentBlockParam> results = new ArrayList<>();
                for (ToolUseBlock use : uses) {
                    JsonNode input = json.valueToTree(use._input().convert(Object.class));
                    ToolOutcome o = runTool(use.name(), input, req.user());
                    results.add(ContentBlockParam.ofToolResult(ToolResultBlockParam.builder()
                            .toolUseId(use.id())
                            .content(o.failed() ? "Lỗi: " + o.error() : o.result())
                            .isError(o.failed())
                            .build()));
                }
                params.addUserMessageOfBlockParams(results);
            }
        } catch (AnthropicServiceException e) {
            throw new IOException("Dịch vụ AI trả về lỗi " + e.statusCode() + hint(e.statusCode()));
        } catch (AnthropicException e) {
            throw new IOException("Không kết nối được dịch vụ AI: " + e.getMessage());
        }
        throw new IOException(TOO_MANY_ROUNDS);
    }

    /** Đổi {@link AiTool} (JSON Schema dạng chuỗi) sang kiểu Tool của SDK. */
    private Tool toClaudeTool(AiTool t) throws IOException {
        Map<String, Object> schema = json.convertValue(schemaOf(t), new TypeReference<>() {
        });
        Tool.InputSchema.Properties.Builder props = Tool.InputSchema.Properties.builder();
        Object properties = schema.get("properties");
        if (properties instanceof Map<?, ?> map) {
            map.forEach((k, v) -> props.putAdditionalProperty(String.valueOf(k), JsonValue.from(v)));
        }
        Tool.InputSchema.Builder input = Tool.InputSchema.builder().properties(props.build());
        if (schema.get("required") instanceof List<?> req) {
            input.required(req.stream().map(String::valueOf).toList());
        }
        return Tool.builder().name(t.name()).description(t.description()).inputSchema(input.build()).build();
    }

    private static List<ChatTurn> merge(List<ChatTurn> turns) {
        List<ChatTurn> out = new ArrayList<>();
        for (ChatTurn t : turns) {
            if (!out.isEmpty() && out.get(out.size() - 1).role().equals(t.role())) {
                ChatTurn last = out.remove(out.size() - 1);
                out.add(new ChatTurn(t.role(), last.content() + "\n" + t.content()));
            } else {
                out.add(t);
            }
        }
        return out;
    }

    private static String hint(int status) {
        return switch (status) {
            case 401 -> " (khóa API sai hoặc hết hạn)";
            case 404 -> " (tên model không tồn tại)";
            case 429 -> " (đang bị giới hạn lượt gọi, thử lại sau)";
            case 529 -> " (dịch vụ đang quá tải, thử lại sau)";
            default -> "";
        };
    }

    /** Tạo (hoặc tạo lại khi đổi khóa) client SDK. synchronized: nhiều request cùng lúc chỉ tạo 1 client. */
    private synchronized AnthropicClient client() {
        String key = settings.claudeKey();
        if (client == null || !key.equals(clientKey)) {
            client = AnthropicOkHttpClient.builder()
                    .apiKey(key)
                    .timeout(Duration.ofSeconds(90))
                    .maxRetries(1)
                    .build();
            clientKey = key;
        }
        return client;
    }
}
