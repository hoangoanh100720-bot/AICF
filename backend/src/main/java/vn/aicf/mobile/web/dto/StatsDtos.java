package vn.aicf.mobile.web.dto;

import vn.aicf.mobile.repository.OrderItemRepository.TopProduct;

import java.time.LocalDate;
import java.util.List;

/** DTO cho thống kê /api/stats (màn hình Tổng quan của Admin). */
public final class StatsDtos {

    private StatsDtos() {
    }

    /** Doanh thu 1 ngày (vẽ biểu đồ cột 7 ngày). */
    public record DayRevenue(LocalDate date, long revenue) {
    }

    /**
     * GET /api/stats/dashboard?date=2026-10-15
     *
     * @param revenue        doanh thu ngày (chỉ đơn hoàn thành)
     * @param orderCount     tổng số đơn tạo trong ngày
     * @param completedCount số đơn hoàn thành
     * @param cancelledCount số đơn bị hủy
     * @param activeCount    số đơn đang xử lý (chưa xong / chưa hủy)
     * @param topProducts    5 món bán chạy nhất trong ngày
     * @param last7Days      doanh thu 7 ngày gần nhất (tính đến ngày được chọn)
     */
    public record Dashboard(LocalDate date, long revenue, long orderCount, long completedCount, long cancelledCount,
                            long activeCount, List<TopProduct> topProducts, List<DayRevenue> last7Days) {
    }
}
