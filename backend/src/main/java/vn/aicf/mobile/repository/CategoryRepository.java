package vn.aicf.mobile.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.aicf.mobile.domain.Category;

/** Truy vấn bảng CATEGORIES. */
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /** Kiểm tra trùng tên khi thêm mới. */
    boolean existsByNameIgnoreCase(String name);

    /** Kiểm tra trùng tên khi sửa (bỏ qua chính bản ghi đang sửa). */
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
