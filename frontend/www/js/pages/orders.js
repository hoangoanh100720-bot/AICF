/**
 * ĐƠN HÀNG CỦA KHÁCH
 *  render()        : danh sách đơn của tôi   (GET /api/orders/my)
 *  renderDetail()  : chi tiết 1 đơn + thanh tiến trình, tự làm mới 15 giây/lần khi đơn chưa xong
 *                    (GET /api/orders/{id}); khách hủy khi còn chờ xác nhận (POST /api/orders/{id}/cancel).
 * Nhân viên mở chi tiết đơn cũng dùng màn này (có thêm nút đổi trạng thái).
 */
import { h } from '../core/dom.js';
import { dateTime, money, NEXT_STATUS, ORDER_FLOW, PAYMENT_LABEL } from '../core/format.js';
import { navigate } from '../core/router.js';
import { appState } from '../core/store.js';
import { orderRepo } from '../data/repositories.js';
import {
  asyncView, confirmDialog, emptyState, iconButton, setHeader, statusChip, toast, toastError,
} from '../ui/components.js';
import { icons } from '../ui/icons.js';

export function render() {
  const view = asyncView(() => orderRepo.my(), (orders) => (orders.length
    ? h('div', { class: 'stack-sm' }, orders.map(orderCard))
    : emptyState('🧾', 'Bạn chưa có đơn nào', 'Đơn đã đặt sẽ hiện ở đây.', h('a', { class: 'btn', href: '#/menu' }, 'Đặt món ngay'))));
  setHeader('Đơn của tôi', iconButton(icons.refresh, 'Làm mới', () => view.reload()));
  return view;
}

/** Thẻ tóm tắt 1 đơn trong danh sách. */
export function orderCard(o) {
  return h('a', { class: 'card card-link stack-sm', href: '#/orders/' + o.id },
    h('div', { class: 'row-between' }, h('strong', null, o.code), statusChip(o.status)),
    h('div', { class: 'muted small' }, o.items.map((i) => `${i.quantity}× ${i.productName}`).join(', ')),
    h('div', { class: 'row-between small' },
      h('span', { class: 'muted' }, dateTime(o.createdAt) + ' · ' + (o.tableName || 'Mang đi')),
      h('strong', { class: 'num' }, money(o.totalAmount))));
}

export function renderDetail(ctx) {
  const id = ctx.params.id;
  const isStaff = appState.get().user.role !== 'CUSTOMER';
  setHeader('Chi tiết đơn', iconButton(icons.back, 'Quay lại', () => history.back()));

  const view = asyncView(() => orderRepo.get(id), (o, reload) => {
    const step = ORDER_FLOW.indexOf(o.status);
    const actions = [];
    if (!isStaff && o.status === 'PENDING') {
      actions.push(h('button', {
        class: 'btn btn-danger btn-block', type: 'button',
        onclick: async () => {
          if (!(await confirmDialog('Hủy đơn này?', { okLabel: 'Hủy đơn', danger: true }))) return;
          try {
            await orderRepo.cancel(o.id, 'Khách tự hủy');
            toast('Đã hủy đơn', 'success');
            reload(true);
          } catch (err) { toastError(err); }
        },
      }, 'Hủy đơn'));
    }
    if (isStaff && NEXT_STATUS[o.status]) {
      const next = NEXT_STATUS[o.status];
      actions.push(h('button', {
        class: 'btn btn-block', type: 'button',
        onclick: async () => {
          try {
            await orderRepo.setStatus(o.id, next.status);
            reload(true);
          } catch (err) { toastError(err); }
        },
      }, next.label));
    }

    return h('div', { class: 'stack' },
      h('section', { class: 'card stack' },
        h('div', { class: 'row-between' }, h('h2', null, o.code), statusChip(o.status)),
        o.status === 'CANCELLED'
          ? h('div', { class: 'banner banner-error' }, 'Đơn đã hủy: ' + (o.cancelReason || ''))
          : h('div', { class: 'stack-sm' },
            h('div', { class: 'progress', role: 'progressbar', 'aria-valuemin': 1, 'aria-valuemax': 5, 'aria-valuenow': step + 1, 'aria-label': 'Tiến trình đơn' },
              ORDER_FLOW.map((_, i) => h('span', { class: i <= step ? 'done' : '' }))),
            h('span', { class: 'muted small' }, o.statusLabel)),
        h('dl', { class: 'stack-sm small', style: { margin: 0 } },
          info('Khách', o.customerName),
          info('Phục vụ', o.tableName ? 'Tại quán · ' + o.tableName : 'Mang đi'),
          info('Thanh toán', PAYMENT_LABEL[o.paymentMethod] + (o.paid ? ' · đã thanh toán' : ' · chưa thanh toán')),
          info('Đặt lúc', dateTime(o.createdAt)),
          o.note ? info('Ghi chú', o.note) : null)),
      h('section', { class: 'card' },
        o.items.map((i) => h('div', { class: 'line' },
          h('span', { class: 'qty num' }, i.quantity + '×'),
          h('div', { class: 'grow' }, h('div', null, i.productName), i.note ? h('div', { class: 'muted small' }, i.note) : null),
          h('span', { class: 'num' }, money(i.lineTotal)))),
        h('div', { class: 'totals', style: { marginTop: 'var(--space-3)' } }, h('span', null, 'Tổng cộng'), h('span', { class: 'num' }, money(o.totalAmount)))),
      actions.length ? h('div', { class: 'stack-sm' }, actions) : null,
      !isStaff ? h('button', { class: 'btn btn-ghost', type: 'button', onclick: () => navigate('/menu') }, 'Đặt thêm món') : null);
  }, { skeletonRows: 2, skeletonHeight: 160 });

  // Đơn chưa xong: tự làm mới để khách thấy trạng thái thay đổi (kể cả khi chưa bật Firebase)
  const timer = setInterval(() => view.reload(true), 15_000);
  ctx.onLeave(() => clearInterval(timer));
  return view;
}

const info = (label, value) => h('div', { class: 'row-between' }, h('dt', { class: 'muted' }, label), h('dd', { style: { margin: 0, textAlign: 'right' } }, value));
