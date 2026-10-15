package vn.aicf.mobile.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.aicf.mobile.domain.OrderItem;

import java.time.LocalDateTime;
import java.util.List;

/** Truy vấn bảng ORDER_ITEMS. */
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /** 1 dòng kết quả "món bán chạy": tên món, tổng số lượng, tổng tiền. */
    record TopProduct(String productName, long quantity, long revenue) {
    }

    /**
     * Món bán chạy trong khoảng [from, to) (chỉ tính đơn hoàn thành).
     * "SELECT new ..." tạo thẳng đối tượng TopProduct từ kết quả GROUP BY (constructor expression của JPQL);
     * lớp lồng bên trong interface được viết là TênNgoài$TênTrong.
     * Pageable dùng để giới hạn số dòng (LIMIT), vd PageRequest.of(0, 5) = top 5.
     */
    @Query("""
            SELECT new vn.aicf.mobile.repository.OrderItemRepository$TopProduct(
                     i.productName, SUM(i.quantity), SUM(i.unitPrice * i.quantity))
            FROM OrderItem i JOIN i.order o
            WHERE o.status = vn.aicf.mobile.domain.OrderStatus.COMPLETED
              AND o.createdAt >= :from AND o.createdAt < :to
            GROUP BY i.productName
            ORDER BY SUM(i.quantity) DESC
            """)
    List<TopProduct> topProducts(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable page);
}
