/**
 * MÀN HÌNH GIỎ HÀNG & ĐẶT ĐƠN.
 * Giỏ lưu trong máy (SQLite / LocalDb). Bấm "Đặt món" -> POST /api/orders với id món + số lượng;
 * máy chủ tự lấy giá thật và tính tổng (không tin giá trên máy).
 */
import { h } from '../core/dom.js';
import { money, PAYMENT_LABEL } from '../core/format.js';
import { navigate } from '../core/router.js';
import { cart } from '../data/cart.js';
import { orderRepo, tableRepo } from '../data/repositories.js';
import { asyncView, emptyState, setHeader, toast, toastError } from '../ui/components.js';

export function render() {
  setHeader('Giỏ hàng');
  const view = asyncView(async () => ({ items: await cart.items(), tables: await tableRepo.list().catch(() => []) }), draw);
  return view;
}

function draw({ items, tables }, reload) {
  if (!items.length) {
    return emptyState('🛒', 'Giỏ hàng đang trống', 'Chọn vài món ngon trong thực đơn nhé!',
      h('a', { class: 'btn', href: '#/menu' }, 'Xem thực đơn'));
  }
  const totalEl = h('span', { class: 'num' }, money(cart.total(items)));

  const lines = items.map((it) => {
    const qty = h('span', null, it.quantity);
    const change = async (delta) => {
      const fresh = await cart.setQuantity(it.productId, it.quantity + delta);
      if (it.quantity + delta <= 0) return reload(true);
      it.quantity += delta;
      qty.textContent = it.quantity;
      totalEl.textContent = money(cart.total(fresh));
    };
    return h('div', { class: 'line' },
      h('div', { class: 'grow stack-sm' },
        h('strong', null, it.name),
        h('span', { class: 'muted small num' }, money(it.price)),
        h('input', {
          class: 'input', type: 'text', placeholder: 'Ghi chú: ít đá, không đường...', value: it.note || '',
          'aria-label': `Ghi chú cho ${it.name}`, style: { minHeight: '36px' },
          onchange: (e) => cart.setNote(it.productId, e.target.value.trim()),
        })),
      h('div', { class: 'stepper', role: 'group', 'aria-label': `Số lượng ${it.name}` },
        h('button', { type: 'button', 'aria-label': 'Bớt 1', onclick: () => change(-1) }, '−'),
        qty,
        h('button', { type: 'button', 'aria-label': 'Thêm 1', onclick: () => change(1) }, '+')));
  });

  // Hình thức phục vụ + bàn + thanh toán
  const activeTables = tables.filter((t) => t.active);
  const tableSelect = h('select', { class: 'input', id: 'table', name: 'tableId' },
    h('option', { value: '' }, '— Chọn bàn —'),
    activeTables.map((t) => h('option', { value: t.id }, `${t.name}${t.area ? ' · ' + t.area : ''} (${t.seats} ghế)`)));
  const tableField = h('div', { class: 'field' }, h('label', { for: 'table' }, 'Bàn của bạn *'), tableSelect);
  const typeSwitch = h('fieldset', { class: 'field' },
    h('legend', { class: 'label', style: { marginBottom: '4px', fontWeight: 600, fontSize: 'var(--text-sm)' } }, 'Hình thức'),
    h('div', { class: 'segmented' },
      [['DINE_IN', 'Uống tại quán'], ['TAKEAWAY', 'Mang đi']].map(([v, label]) =>
        h('label', null, h('input', {
          type: 'radio', name: 'orderType', value: v, checked: v === 'DINE_IN',
          onchange: () => { tableField.hidden = v !== 'DINE_IN'; },
        }), h('span', null, label)))));
  const payment = h('select', { class: 'input', id: 'payment', name: 'paymentMethod' },
    Object.entries(PAYMENT_LABEL).map(([v, l]) => h('option', { value: v }, l)));
  const note = h('textarea', { class: 'input', id: 'order-note', name: 'note', placeholder: 'Ghi chú cho quán (không bắt buộc)', maxlength: 500 });

  const submit = h('button', { class: 'btn btn-block', type: 'submit' }, 'Đặt món');
  const form = h('form', { class: 'stack', novalidate: true },
    typeSwitch, tableField,
    h('div', { class: 'field' }, h('label', { for: 'payment' }, 'Thanh toán'), payment),
    h('div', { class: 'field' }, h('label', { for: 'order-note' }, 'Ghi chú'), note),
    h('div', { class: 'totals' }, h('span', null, 'Tạm tính'), totalEl),
    h('p', { class: 'muted small' }, 'Giá cuối cùng do quán xác nhận khi đặt.'),
    submit);

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const orderType = form.elements.orderType.value;
    if (orderType === 'DINE_IN' && !tableSelect.value) {
      toast('Vui lòng chọn bàn', 'error');
      tableSelect.focus();
      return;
    }
    submit.disabled = true;
    try {
      const current = await cart.items(); // đọc lại giỏ (ghi chú vừa sửa)
      const order = await orderRepo.create({
        orderType,
        tableId: orderType === 'DINE_IN' ? Number(tableSelect.value) : null,
        paymentMethod: payment.value,
        note: note.value.trim() || null,
        items: current.map((i) => ({ productId: i.productId, quantity: i.quantity, note: i.note })),
      });
      await cart.clear();
      toast(`Đã đặt đơn ${order.code}!`, 'success');
      navigate('/orders/' + order.id);
    } catch (err) {
      toastError(err);
    } finally {
      submit.disabled = false;
    }
  });

  return h('div', { class: 'stack' },
    h('section', { class: 'card', 'aria-label': 'Các món đã chọn' }, lines,
      h('button', {
        class: 'btn btn-ghost btn-sm', type: 'button', style: { marginTop: 'var(--space-2)' },
        onclick: async () => { await cart.clear(); reload(true); },
      }, 'Xóa hết giỏ')),
    h('section', { class: 'card' }, form));
}

