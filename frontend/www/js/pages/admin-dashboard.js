/**
 * TỔNG QUAN (ADMIN): GET /api/stats/dashboard?date=YYYY-MM-DD
 *  - Doanh thu ngày (con số lớn), số đơn, hoàn thành, hủy, đang xử lý
 *  - Biểu đồ cột doanh thu 7 ngày (1 chuỗi số liệu, 1 màu; di chuột / chạm vào cột để xem số; có bảng cho trình đọc màn hình)
 *  - 5 món bán chạy
 */
import { h } from '../core/dom.js';
import { money, todayIso } from '../core/format.js';
import { statsRepo } from '../data/repositories.js';
import { asyncView, emptyState, setHeader } from '../ui/components.js';

export function render(ctx) {
  let date = ctx.query.date || todayIso();
  const picker = h('input', {
    class: 'input', type: 'date', value: date, max: todayIso(), 'aria-label': 'Chọn ngày', style: { width: 'auto', minHeight: '36px' },
    onchange: (e) => { date = e.target.value || todayIso(); view.reload(); },
  });
  setHeader('Tổng quan', picker);

  const view = asyncView(() => statsRepo.dashboard(date), (d) => h('div', { class: 'stack' },
    h('div', { class: 'stats-grid' },
      stat('Doanh thu ' + (d.date === todayIso() ? 'hôm nay' : formatDay(d.date)), money(d.revenue), true),
      stat('Tổng đơn', d.orderCount),
      stat('Hoàn thành', d.completedCount),
      stat('Đang xử lý', d.activeCount),
      stat('Đã hủy', d.cancelledCount)),
    h('section', { class: 'card' },
      h('div', { class: 'section-title' }, h('h2', null, 'Doanh thu 7 ngày'), h('span', { class: 'muted small' }, 'Chỉ tính đơn hoàn thành')),
      revenueChart(d.last7Days)),
    h('section', { class: 'card stack-sm' },
      h('h2', { style: { fontSize: 'var(--text-lg)' } }, 'Món bán chạy'),
      d.topProducts.length
        ? h('div', null, d.topProducts.map((t, i) => h('div', { class: 'line' },
          h('span', { class: 'qty num' }, '#' + (i + 1)),
          h('span', { class: 'grow' }, t.productName),
          h('span', { class: 'muted small num' }, t.quantity + ' ly · ' + money(t.revenue)))))
        : emptyState('📊', 'Chưa có đơn hoàn thành trong ngày'))),
  { skeletonRows: 3, skeletonHeight: 120 });
  return view;
}

const stat = (label, value, primary = false) =>
  h('div', { class: 'stat' + (primary ? ' primary' : '') }, h('div', { class: 'label' }, label), h('div', { class: 'value' }, value));

const formatDay = (iso) => iso.slice(8, 10) + '/' + iso.slice(5, 7);

/**
 * Biểu đồ cột đơn giản bằng HTML/CSS (không cần thư viện): chiều cao cột = doanh thu / doanh thu lớn nhất.
 * Chỉ ghi số trên cột của ngày đang xem (tránh rối mắt); các cột khác xem số bằng cách di chuột / chạm (title).
 * Có thêm bảng ẩn cho trình đọc màn hình.
 */
function revenueChart(days) {
  const max = Math.max(...days.map((d) => d.revenue), 1);
  const last = days.length - 1;
  return h('figure', { style: { margin: 0 } },
    h('div', { class: 'bars', 'aria-hidden': 'true' }, days.map((d, i) => {
      const pct = (d.revenue / max) * 100;
      return h('div', { class: 'bar' + (i === last ? ' today' : ''), title: `${formatDay(d.date)}: ${money(d.revenue)}` },
        i === last && d.revenue > 0 ? h('span', { class: 'val', style: { bottom: `calc(${pct}% + 4px)` } }, shortMoney(d.revenue)) : null,
        h('div', { class: 'fill', style: { height: pct + '%' } }));
    })),
    h('div', { class: 'bar-labels', 'aria-hidden': 'true' }, days.map((d) => h('span', null, formatDay(d.date)))),
    h('table', { class: 'sr-only' },
      h('caption', null, 'Doanh thu 7 ngày'),
      h('tbody', null, days.map((d) => h('tr', null, h('th', null, formatDay(d.date)), h('td', null, money(d.revenue)))))));
}

/** 1250000 -> "1,3tr", 85000 -> "85k" */
function shortMoney(v) {
  if (v >= 1_000_000) return (v / 1_000_000).toFixed(1).replace('.', ',').replace(',0', '') + 'tr';
  if (v >= 1000) return Math.round(v / 1000) + 'k';
  return String(v);
}
