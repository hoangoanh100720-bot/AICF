/**
 * BẢNG ĐƠN CỦA NHÂN VIÊN: các đơn đang xử lý (GET /api/orders/active), đơn đến trước đứng trước.
 * Mỗi thẻ có 1 nút "bước tiếp theo" (Nhận đơn -> Bắt đầu pha -> Đã xong -> Đã giao & thu tiền)
 * gọi PATCH /api/orders/{id}/status; khách được báo ngay (thông báo trong app + FCM).
 * Tự làm mới 10 giây/lần. Tab "Hôm nay" xem toàn bộ đơn trong ngày.
 */
import { BOARD_REFRESH_MS } from '../config.js';
import { h, mount } from '../core/dom.js';
import { dateTime, money, NEXT_STATUS, todayIso } from '../core/format.js';
import { orderRepo } from '../data/repositories.js';
import { asyncView, confirmDialog, emptyState, iconButton, setHeader, statusChip, toast, toastError } from '../ui/components.js';
import { icons } from '../ui/icons.js';
import { orderCard } from './orders.js';

export function render(ctx) {
  let tab = ctx.query.tab === 'today' ? 'today' : 'active';
  const box = h('div');
  let view;

  const draw = () => {
    view = tab === 'active'
      ? asyncView(() => orderRepo.active(), (orders, reload) => (orders.length
        ? h('div', { class: 'stack-sm' }, orders.map((o) => boardCard(o, reload)))
        : emptyState('☕', 'Chưa có đơn nào cần làm', 'Đơn mới sẽ tự hiện ở đây.')))
      : asyncView(() => orderRepo.ofDay(todayIso()), (orders) => (orders.length
        ? h('div', { class: 'stack-sm' }, orders.map(orderCard))
        : emptyState('🧾', 'Hôm nay chưa có đơn')));
    mount(box, view);
  };

  const tabs = h('div', { class: 'segmented', role: 'radiogroup', 'aria-label': 'Chọn danh sách' },
    [['active', 'Đang xử lý'], ['today', 'Tất cả hôm nay']].map(([v, label]) =>
      h('label', null, h('input', { type: 'radio', name: 'board-tab', value: v, checked: v === tab, onchange: () => { tab = v; draw(); } }),
        h('span', null, label))));

  setHeader('Đơn hàng', iconButton(icons.refresh, 'Làm mới', () => view?.reload()));
  draw();
  const timer = setInterval(() => { if (!document.hidden) view?.reload(true); }, BOARD_REFRESH_MS);
  ctx.onLeave(() => clearInterval(timer));
  return h('div', { class: 'stack' }, tabs, box);
}

/** Thẻ 1 đơn trên bảng nhân viên, có nút chuyển bước và nút hủy. */
function boardCard(o, reload) {
  const next = NEXT_STATUS[o.status];
  const act = async (btn, status, reason) => {
    btn.disabled = true;
    try {
      await orderRepo.setStatus(o.id, status, reason);
      toast(`${o.code}: đã cập nhật`, 'success', 1500);
      reload(true);
    } catch (err) {
      toastError(err);
      btn.disabled = false;
    }
  };
  return h('article', { class: 'card board-card stack-sm' },
    h('div', { class: 'row-between' },
      h('a', { href: '#/orders/' + o.id, style: { fontWeight: 700, color: 'inherit' } }, o.code),
      statusChip(o.status)),
    h('div', { class: 'muted small' },
      (o.tableName ? '🪑 ' + o.tableName : '🥡 Mang đi') + ' · ' + o.customerName + ' · ' + dateTime(o.createdAt)),
    h('ul', { class: 'items', style: { margin: 0, paddingLeft: '1.2em' } },
      o.items.map((i) => h('li', null, h('strong', null, i.quantity + '× '), i.productName, i.note ? h('span', { class: 'muted' }, ' — ' + i.note) : null))),
    o.note ? h('div', { class: 'banner banner-info' }, '📝 ' + o.note) : null,
    h('div', { class: 'row-between' },
      h('span', { class: 'muted small' }, o.paid ? 'Đã thanh toán' : 'Chưa thanh toán'),
      h('strong', { class: 'num' }, money(o.totalAmount))),
    h('div', { class: 'actions' },
      h('button', {
        class: 'btn btn-secondary btn-sm', type: 'button',
        onclick: async (e) => {
          const btn = e.currentTarget;
          if (await confirmDialog(`Hủy đơn ${o.code}? Khách sẽ nhận được thông báo.`, { okLabel: 'Hủy đơn', danger: true })) {
            act(btn, 'CANCELLED', 'Quán hết nguyên liệu / không phục vụ được');
          }
        },
      }, 'Hủy'),
      next ? h('button', { class: 'btn btn-sm', type: 'button', onclick: (e) => act(e.currentTarget, next.status) }, next.label) : null));
}
