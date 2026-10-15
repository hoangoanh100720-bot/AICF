package vn.aicf.mobile.common;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Chuyển LocalDateTime (Java) <-> TEXT 'yyyy-MM-dd HH:mm:ss' (SQLite).
 *
 * <p>Vì sao cần: SQLite không có kiểu ngày giờ. Nếu để driver tự lưu, nó có thể ghi số mili-giây,
 * khó đọc khi mở file .db và lệch với giá trị mặc định datetime('now','localtime') trong schema.sql.
 * Lớp này bảo đảm mọi cột ngày giờ đều cùng một định dạng chữ, đọc được bằng mắt, và
 * so sánh theo chữ cũng đúng thứ tự thời gian (dùng cho thống kê theo ngày).
 *
 * <p>{@code autoApply = true}: áp dụng tự động cho MỌI thuộc tính kiểu LocalDateTime trong mọi Entity,
 * kể cả tham số truy vấn so sánh với các thuộc tính đó.
 */
@Converter(autoApply = true)
public class LocalDateTimeConverter implements AttributeConverter<LocalDateTime, String> {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** Java -> CSDL. */
    @Override
    public String convertToDatabaseColumn(LocalDateTime value) {
        return value == null ? null : value.format(FORMAT);
    }

    /** CSDL -> Java. Chấp nhận cả dạng có chữ 'T' hoặc phần lẻ giây phòng khi dữ liệu nhập tay. */
    @Override
    public LocalDateTime convertToEntityAttribute(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String s = text.trim().replace('T', ' ');
        if (s.length() > 19) {
            s = s.substring(0, 19);      // bỏ phần mili-giây nếu có
        }
        if (s.length() == 10) {
            s = s + " 00:00:00";         // chỉ có ngày
        }
        return LocalDateTime.parse(s, FORMAT);
    }
}
