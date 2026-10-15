/**
 * HỘP THƯ THÔNG BÁO (GET /api/notifications). Bấm 1 thông báo: đánh dấu đã đọc và mở đơn liên quan.
 * Thông báo luôn được lưu ở máy chủ nên vẫn xem được dù chưa bật Firebase / đã tắt thông báo đẩy.
 */
import { h } from '../core/dom.js';
import { dateTime } from '../core/format.js';
import { navigate } from '../core/router.js';
import { appState } from '../core/store.js';
import { notificationRepo } from '../data/repositories.js';
import { asyncView, emptyState, setHeader, toastError } from '../ui/components.js';

export function render() {
  const view = asyncView(() => notificationRepo.list(), (items, reload) => {
    if (!items.length) return emptyState('🔔', 'Chưa có thông báo', 'Khi đơn hàng đổi trạng thái, bạn sẽ nhận thông báo ở đây.');
    const unreadCount = items.filter((n) => !n.read).length;
    return h('div', { class: 'stack-sm' },
      unreadCount ? h('div', { class: 'row-between' },
        h('span', { class: 'muted small' }, `${unreadCount} chưa đọc`),
        h('button', {
          class: 'btn btn-ghost btn-sm', type: 'button',
          onclick: async () => {
            try {
              await notificationRepo.readAll();
              appState.set({ unread: 0 });
              reload(true);
            } catch (err) { toastError(err); }
          },
        }, 'Đánh dấu đã đọc tất cả')) : null,
      h('div', { class: 'list' }, items.map((n) => h('button', {
        class: 'list-item' + (n.read ? '' : ' unread'), type: 'button',
        onclick: async () => {
          if (!n.read) {
            await notificationRepo.read(n.id).catch(() => {});
            appState.set({ unread: Math.max(0, appState.get().unread - 1) });
          }
          if (n.orderId) navigate('/orders/' + n.orderId);
          else reload(true);
        },
      }, h('div', { class: 'grow' },
        h('div', { class: 'title' }, n.title),
        h('div', { class: 'sub' }, n.body),
        h('div', { class: 'sub small' }, dateTime(n.createdAt)))))));
  });
  setHeader('Thông báo');
  return view;
}
