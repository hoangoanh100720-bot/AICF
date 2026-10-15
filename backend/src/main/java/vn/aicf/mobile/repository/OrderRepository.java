package vn.aicf.mobile.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.aicf.mobile.domain.CustomerOrder;
import vn.aicf.mobile.domain.OrderStatus;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/** Truy vấn bảng ORDERS (+ thống kê doanh thu). */
public interface OrderRepository extends JpaRepository<CustomerOrder, Long> {

    /** Đơn của 1 khách, mới nhất trước. */
    List<CustomerOrder> findByUserIdOrderByIdDesc(Long userId);

    /** Đơn theo danh sách trạng thái, cũ nhất trước (màn hình nhân viên: đơn nào đến trước làm trước). */
    List<CustomerOrder> findByStatusInOrderByIdAsc(Collection<OrderStatus> statuses);

    /** Tất cả đơn trong khoảng [from, to), mới nhất trước. */
    List<CustomerOrder> findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByIdDesc(LocalDateTime from, LocalDateTime to);

    /** Số đơn đã tạo trong khoảng [from, to) - dùng để sinh số thứ tự trong mã đơn. */
    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(LocalDateTime from, LocalDateTime to);

    /** Số đơn theo trạng thái trong khoảng [from, to). */
    long countByStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(OrderStatus status, LocalDateTime from, LocalDateTime to);

    /**
     * Doanh thu = tổng tiền các đơn HOÀN THÀNH trong khoảng [from, to).
     * COALESCE(..., 0): không có đơn nào thì SUM trả NULL -> đổi thành 0.
     */
    @Query("""
            SELECT COALESCE(SUM(o.totalAmount), 0) FROM CustomerOrder o
            WHERE o.status = vn.aicf.mobile.domain.OrderStatus.COMPLETED
              AND o.createdAt >= :from AND o.createdAt < :to
            """)
    long revenueBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
