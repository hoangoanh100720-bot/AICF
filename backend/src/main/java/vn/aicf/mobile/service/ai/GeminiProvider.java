package vn.aicf.mobile.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Google GEMINI qua REST API generateContent (function calling) - lấy lại từ dự án AICF.
 * Gemini chưa có SDK Java chính thức gọn nhẹ nên gọi thẳng HTTP bằng java.net.http.HttpClient (có sẵn trong JDK).
 *
 * <p>Lưu ý đã kiểm chứng với dịch vụ thật: kết quả hàm gửi với role "user", và phải GIỮ NGUYÊN "parts" của lượt model
 * (có "thoughtSignature") khi gửi lại, nếu không dịch vụ báo lỗi 400.
 */
@Component
public class GeminiProvider extends AbstractLlmProvider {

    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final int MAX_OUTPUT_TOKENS = 2048;

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public GeminiProvider(AiToolRegistry tools, AiSettings settings, ObjectMapper json) {
        super(tools, settings, json);
    }

    @Override
    public String id() {
        return "gemini";
    }

    @Override
    public boolean hasKey() {
        return !settings.geminiKey().isBlank();
    }

    @Override
    public String model() {
        return settings.geminiModel();
    }

    @Override
    public String chat(LlmRequest req) throws IOException, InterruptedException {
        String url = BASE_URL + model() + ":generateContent";

        // 1. Khai báo công cụ (functionDeclarations)
        ArrayNode decls = json.createArrayNode();
        for (AiTool t : req.tools()) {
            ObjectNode d = decls.addObject();
            d.put("name", t.name()).put("description", t.description());
            d.set("parameters", schemaOf(t));
        }
        ArrayNode toolDefs = json.createArrayNode();
        toolDefs.addObject().set("functionDeclarations", decls);

        // 2. Lịch sử + câu hỏi. Gemini gọi vai trò trợ lý là "model"
        ArrayNode contents = json.createArrayNode();
        for (ChatTurn t : recent(req.history())) {
            appendText(contents, t.fromUser() ? "user" : "model", t.content());
        }
        appendText(contents, "user", req.message());

        // 3. Vòng lặp: gửi -> nếu AI muốn gọi hàm thì chạy hàm, gửi kết quả -> lặp lại tới khi AI trả lời bằng chữ
        for (int round = 0; round < MAX_ROUNDS; round++) {
            ObjectNode body = json.createObjectNode();
            body.putObject("system_instruction").putArray("parts").addObject().put("text", req.systemPrompt());
            body.putObject("generationConfig").put("maxOutputTokens", MAX_OUTPUT_TOKENS);
            body.set("tools", toolDefs);
            body.set("contents", contents);
            JsonNode resp = postJson(url, body);

            JsonNode parts = resp.path("candidates").path(0).path("content").path("parts");
            StringBuilder text = new StringBuilder();
            List<JsonNode> calls = new ArrayList<>();
            for (JsonNode part : parts) {
                if (part.has("functionCall")) {
                    calls.add(part.path("functionCall"));
                } else if (part.has("text")) {
                    text.append(part.path("text").asText());
                }
            }
            if (calls.isEmpty()) {
                String out = text.toString().trim();
                if (out.isEmpty()) {
                    String block = resp.path("promptFeedback").path("blockReason").asText("");
                    throw new IOException(block.isBlank() ? "Dịch vụ AI không trả về nội dung" : "Dịch vụ AI từ chối trả lời (" + block + ")");
                }
                return out;
            }
            contents.addObject().put("role", "model").set("parts", parts);
            ArrayNode resultParts = contents.addObject().put("role", "user").putArray("parts");
            for (JsonNode call : calls) {
                String name = call.path("name").asText();
                ToolOutcome o = runTool(name, call.path("args"), req.user());
                ObjectNode response = json.createObjectNode();
                if (o.failed()) {
                    response.put("error", o.error());
                } else {
                    response.put("result", o.result());
                }
                ObjectNode fr = resultParts.addObject().putObject("functionResponse");
                fr.put("name", name);
                fr.set("response", response);
            }
        }
        throw new IOException(TOO_MANY_ROUNDS);
    }

    /** Hai tin liên tiếp cùng vai trò thì gộp lại (dịch vụ yêu cầu xen kẽ user / model). */
    private void appendText(ArrayNode contents, String role, String text) {
        if (!contents.isEmpty() && role.equals(contents.get(contents.size() - 1).path("role").asText())) {
            JsonNode lastParts = contents.get(contents.size() - 1).path("parts");
            if (lastParts.size() == 1 && lastParts.get(0).has("text")) {
                ((ObjectNode) lastParts.get(0)).put("text", lastParts.get(0).path("text").asText() + "\n" + text);
                return;
            }
        }
        contents.addObject().put("role", role).putArray("parts").addObject().put("text", text);
    }

    /** Gửi JSON, đổi lỗi HTTP thành thông báo tiếng Việt dễ hiểu. */
    private JsonNode postJson(String url, ObjectNode body) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(60))
                .header("content-type", "application/json")
                .header("x-goog-api-key", settings.geminiKey())
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)))
                .build();
        HttpResponse<String> res;
        try {
            res = http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (HttpTimeoutException e) {
            throw new IOException("Dịch vụ AI phản hồi quá lâu, thử lại sau");
        }
        if (res.statusCode() / 100 != 2) {
            String detail = "";
            try {
                detail = json.readTree(res.body()).path("error").path("message").asText("");
            } catch (Exception ignore) {
                // thân phản hồi không phải JSON
            }
            String hint = switch (res.statusCode()) {
                case 400, 401, 403 -> " (khóa API sai hoặc chưa được cấp quyền)";
                case 404 -> " (tên model không tồn tại)";
                case 429 -> " (đang bị giới hạn lượt gọi, thử lại sau)";
                default -> "";
            };
            throw new IOException("Dịch vụ AI trả về lỗi " + res.statusCode() + hint + (detail.isBlank() ? "" : ": " + detail));
        }
        return json.readTree(res.body());
    }
}
