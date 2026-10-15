package vn.aicf.mobile.repository;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.aicf.mobile.domain.Product;

import java.util.List;

/** Truy vấn bảng PRODUCTS. */
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Ghi đè findAll để nạp luôn danh mục bằng 1 câu JOIN (EntityGraph),
     * tránh lỗi "N+1 truy vấn" (mỗi món lại chạy thêm 1 câu lấy danh mục).
     */
    @Override
    @EntityGraph(attributePaths = "category")
    List<Product> findAll(Sort sort);

    /**
     * Tìm món có lọc - viết bằng JPQL (truy vấn trên Entity, không phải trên bảng).
     * Tham số null nghĩa là "không lọc theo tiêu chí đó".
     *
     * @param categoryId    lọc theo danh mục (null = mọi danh mục)
     * @param keyword       từ khóa trong tên món, ĐÃ đổi chữ thường (null = không lọc)
     * @param onlyAvailable true = chỉ lấy món đang bán
     */
    @Query("""
            SELECT p FROM Product p JOIN FETCH p.category c
            WHERE (:categoryId IS NULL OR c.id = :categoryId)
              AND (:keyword IS NULL OR LOWER(p.name) LIKE CONCAT('%', :keyword, '%'))
              AND (:onlyAvailable = FALSE OR p.available = TRUE)
            ORDER BY c.sortOrder, p.name
            """)
    List<Product> search(@Param("categoryId") Long categoryId,
                         @Param("keyword") String keyword,
                         @Param("onlyAvailable") boolean onlyAvailable);

    /** Đếm món trong 1 danh mục (không cho xóa danh mục còn món). */
    long countByCategoryId(Long categoryId);
}
