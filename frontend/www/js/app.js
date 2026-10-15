/**
 * ĐIỂM KHỞI ĐỘNG CỦA APP.
 *
 * Thứ tự: mở LocalDb (SQLite / localStorage) -> nạp phiên đăng nhập -> dựng router + thanh tab
 * -> đăng ký service worker (PWA) -> định kỳ đếm thông báo chưa đọc.
 *
 * Cấu trúc thư mục js/:
 *   core/      hạ tầng dùng chung, không biết gì về nghiệp vụ (HTTP, router, store, DOM, định dạng)
 *   data/      truy cập dữ liệu: REST repository, phiên đăng nhập, giỏ hàng, CSDL cục bộ
 *   firebase/  đăng nhập Google + thông báo đẩy
 *   ui/        component giao diện tái sử dụng + trang CRUD tổng quát
 *   pages/     từng màn hình (mỗi file export hàm render)
 */
import { IS_NATIVE, UNREAD_POLL_MS } from './config.js';
import { h, mount } from './core/dom.js';
import { Router } from './core/router.js';
import { appState } from './core/store.js';
import { cart } from './data/cart.js';
import { initLocalDb } from './data/local-db.js';
import { notificationRepo, settingsRepo } from './data/repositories.js';
import { session } from './data/session.js';
import { errorState } from './ui/components.js';
import { icons } from './ui/icons.js';

/* ------------------------------ Khai báo màn hình ------------------------------ */

const STAFF = ['STAFF', 'ADMIN'];
const routes = [
  { path: '/login', load: () => import('./pages/login.js'), guestOnly: true, noTabs: true },
  { path: '/menu', load: () => import('./pages/menu.js'), auth: true, tab: 'menu' },
  { path: '/cart', load: () => import('./pages/cart.js'), auth: true, tab: 'cart' },
  { path: '/orders', load: () => import('./pages/orders.js'), auth: true, tab: 'orders' },
  { path: '/orders/:id', load: () => import('./pages/orders.js'), export: 'renderDetail', auth: true, tab: 'orders' },
  { path: '/chat', load: () => import('./pages/chat.js'), auth: true, tab: 'chat' },
  { path: '/notifications', load: () => import('./pages/notifications.js'), auth: true, tab: 'profile' },
  { path: '/profile', load: () => import('./pages/profile.js'), auth: true, tab: 'profile' },
  { path: '/board', load: () => import('./pages/staff-board.js'), auth: true, roles: STAFF, tab: 'board' },
  { path: '/admin', load: () => import('./pages/admin-dashboard.js'), auth: true, roles: ['ADMIN'], tab: 'admin' },
  { path: '/admin/manage', load: () => import('./pages/admin-manage.js'), auth: true, roles: ['ADMIN'], tab: 'manage' },
  { path: '/admin/users', load: () => import('./pages/admin-users.js'), auth: true, roles: ['ADMIN'], tab: 'profile' },
  { path: '/admin/settings', load: () => import('./pages/admin-settings.js'), auth: true, roles: ['ADMIN'], tab: 'profile' },
];

/** Trang chủ theo vai trò. */
const homeFor = (user) => (!user ? '/login' : user.role === 'ADMIN' ? '/admin' : user.role === 'STAFF' ? '/board' : '/menu');

/** Thanh tab dưới theo vai trò: [khóa, đường dẫn, nhãn, biểu tượng, tên trường đếm huy hiệu]. */
const TABS = {
  CUSTOMER: [
    ['menu', '/menu', 'Thực đơn', icons.menu],
    ['cart', '/cart', 'Giỏ hàng', icons.cart, 'cartCount'],
    ['orders', '/orders', 'Đơn hàng', icons.orders],
    ['chat', '/chat', 'Trợ lý AI', icons.chat],
    ['profile', '/profile', 'Tài khoản', icons.user, 'unread'],
  ],
  STAFF: [
    ['board', '/board', 'Đơn cần làm', icons.board],
    ['menu', '/menu', 'Thực đơn', icons.menu],
    ['chat', '/chat', 'Trợ lý AI', icons.chat],
    ['profile', '/profile', 'Tài khoản', icons.user, 'unread'],
  ],
  ADMIN: [
    ['admin', '/admin', 'Tổng quan', icons.chart],
    ['board', '/board', 'Đơn hàng', icons.board],
    ['manage', '/admin/manage', 'Quản lý', icons.settings],
    ['chat', '/chat', 'Trợ lý AI', icons.chat],
    ['profile', '/profile', 'Tài khoản', icons.user, 'unread'],
  ],
};

let currentTab = null;
const tabbar = document.getElementById('tabbar');
const main = document.getElementById('view');

/** Vẽ lại thanh tab (gọi khi đổi màn hình / đổi người dùng / đổi số huy hiệu). */
function renderTabs(hidden) {
  const { user } = appState.get();
  tabbar.hidden = hidden || !user;
  main.classList.toggle('no-tabs', tabbar.hidden);
  if (tabbar.hidden) return;
  const state = appState.get();
  mount(tabbar, (TABS[user.role] || TABS.CUSTOMER).map(([key, path, label, icon, countKey]) => {
    const count = countKey ? state[countKey] : 0;
    return h('a', { class: 'tab', href: '#' + path, 'aria-current': key === currentTab ? 'page' : null },
      h('span', { html: icon, style: { display: 'contents' } }),
      label,
      count > 0 ? h('span', { class: 'badge', 'aria-label': `${count} mục` }, count > 99 ? '99+' : count) : null);
  }));
}

/* ------------------------------ Khởi động ------------------------------ */

const router = new Router({
  outlet: main,
  routes,
  getUser: () => appState.get().user,
  homeFor,
  onChange: (route) => {
    currentTab = route.tab || null;
    renderTabs(route.noTabs);
  },
  renderError: (err) => errorState(err, () => router.resolve()),
});

/** Đếm thông báo chưa đọc (dự phòng cho khi chưa bật Firebase; có FCM thì thông báo vẫn hiện ngay). */
async function refreshUnread() {
  if (!appState.get().user || document.hidden) return;
  try {
    const { unread } = await notificationRepo.unreadCount();
    if (unread !== appState.get().unread) appState.set({ unread });
  } catch { /* mất mạng: bỏ qua */ }
}

async function start() {
  await initLocalDb();
  await session.load();
  await cart.refresh();

  // Đổi người dùng / số giỏ / số chưa đọc -> vẽ lại tab
  let lastUser = appState.get().user;
  appState.subscribe((s) => {
    renderTabs(tabbar.hidden && !s.user);
    if (s.user !== lastUser) {
      lastUser = s.user;
      if (s.user) refreshUnread();
    }
  });

  router.resolve();
  settingsRepo.shop().then((shop) => appState.set({ shop })).catch(() => {});
  refreshUnread();
  setInterval(refreshUnread, UNREAD_POLL_MS);
  document.addEventListener('visibilitychange', refreshUnread);

  // PWA: đăng ký service worker (trong app Android không cần vì giao diện đã nằm sẵn trong máy)
  if ('serviceWorker' in navigator && !IS_NATIVE && window.isSecureContext) {
    navigator.serviceWorker.register('sw.js').catch((e) => console.warn('Không đăng ký được service worker', e));
  }
  // App Android: nút Back cứng -> quay lại màn trước, ở màn đầu thì thoát app
  window.Capacitor?.Plugins?.App?.addListener('backButton', ({ canGoBack }) => {
    if (canGoBack && location.hash !== '#' + homeFor(appState.get().user)) history.back();
    else window.Capacitor.Plugins.App.exitApp();
  });
}

start();
