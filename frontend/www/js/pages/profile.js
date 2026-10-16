/**
 * MÀN HÌNH TÀI KHOẢN: hồ sơ, đổi mật khẩu, bật thông báo đẩy, thông báo, địa chỉ máy chủ (app Android),
 * lối vào các màn quản trị (Admin), đăng xuất.
 */
import { IS_NATIVE } from '../config.js';
import { h } from '../core/dom.js';
import { ROLE_LABEL } from '../core/format.js';
import { navigate } from '../core/router.js';
import { appState } from '../core/store.js';
import { localDb } from '../data/local-db.js';
import { settingsRepo, userRepo } from '../data/repositories.js';
import { session } from '../data/session.js';
import { firebaseSignOut } from '../firebase/firebase.js';
import { disablePush, enablePush, pushAvailable } from '../firebase/push.js';
import { confirmDialog, formSheet, setHeader, toast, toastError } from '../ui/components.js';
import { icons } from '../ui/icons.js';

export function render(ctx) {
  setHeader('Tài khoản');
  const user = appState.get().user;

  const unreadBadge = h('span', { class: 'badge', hidden: !appState.get().unread }, appState.get().unread);
  ctx.onLeave(appState.subscribe(({ unread }) => { unreadBadge.hidden = !unread; unreadBadge.textContent = unread; }));

  const item = (label, sub, onclick, extra) => h('button', { class: 'list-item', type: 'button', onclick },
    h('div', { class: 'grow' }, h('div', { class: 'title' }, label), sub ? h('div', { class: 'sub' }, sub) : null),
    extra || null, h('span', { html: icons.chevron, style: { width: '20px', color: 'var(--muted)', display: 'inline-flex' } }));

  const editProfile = () => formSheet({
    title: 'Sửa hồ sơ',
    fields: [
      { name: 'fullName', label: 'Họ tên', required: true },
      { name: 'email', label: 'Email', type: 'email' },
      { name: 'phone', label: 'Số điện thoại', type: 'tel' },
    ],
    initial: appState.get().user,
    onSubmit: async (data) => {
      await session.setUser(await userRepo.updateMe(data));
      toast('Đã lưu hồ sơ', 'success');
      navigate('/profile?' + Date.now()); // vẽ lại màn hình
    },
  });

  const changePassword = () => formSheet({
    title: user.hasPassword ? 'Đổi mật khẩu' : 'Đặt mật khẩu',
    fields: [
      user.hasPassword ? { name: 'currentPassword', label: 'Mật khẩu hiện tại', type: 'password', required: true, autocomplete: 'current-password' } : null,
      { name: 'newPassword', label: 'Mật khẩu mới', type: 'password', required: true, minLength: 6, autocomplete: 'new-password' },
    ].filter(Boolean),
    submitLabel: 'Đổi mật khẩu',
    onSubmit: async (d) => {
      await userRepo.changePassword(d.currentPassword, d.newPassword);
      toast('Đã đổi mật khẩu', 'success');
    },
  });

  const turnOnPush = async () => {
    try {
      if (await enablePush((orderId) => navigate('/orders/' + orderId))) toast('Đã bật thông báo đẩy', 'success');
    } catch (err) {
      toastError(err);
    }
  };

  const changeServer = () => formSheet({
    title: 'Địa chỉ máy chủ',
    fields: [{ name: 'url', label: 'URL máy chủ', type: 'url', required: true, hint: 'vd http://192.168.1.10:8090' }],
    initial: { url: session.serverUrl() },
    onSubmit: async ({ url }) => {
      await session.setServerUrl(url);
      await settingsRepo.health();
      toast('Kết nối máy chủ thành công', 'success');
    },
  });

  const logout = async () => {
    if (!(await confirmDialog('Đăng xuất khỏi tài khoản này?', { okLabel: 'Đăng xuất' }))) return;
    await disablePush();
    await firebaseSignOut();
    await localDb.cartClear();
    await session.signOut();
    navigate('/menu'); // về thực đơn ở chế độ khách vãng lai
  };

  return h('div', { class: 'stack' },
    h('section', { class: 'card row' },
      user.avatarUrl
        ? h('img', { src: user.avatarUrl, alt: '', style: { width: '56px', height: '56px', borderRadius: '50%' }, referrerpolicy: 'no-referrer' })
        : h('div', { style: { width: '56px', height: '56px', borderRadius: '50%', background: 'var(--accent-soft)', color: 'var(--accent-soft-fg)', display: 'grid', placeItems: 'center', fontSize: 'var(--text-xl)', fontWeight: 700 } },
          (user.fullName || '?').trim().charAt(0).toUpperCase()),
      h('div', { class: 'grow' },
        h('h2', { style: { fontSize: 'var(--text-lg)' } }, user.fullName),
        h('div', { class: 'muted small' }, '@' + user.username + ' · ' + ROLE_LABEL[user.role]))),

    h('div', { class: 'list' },
      item('Thông báo', 'Trạng thái đơn hàng, đơn mới', () => navigate('/notifications'), unreadBadge),
      user.role === 'CUSTOMER' ? item('Đơn của tôi', null, () => navigate('/orders')) : null,
      item('Sửa hồ sơ', [user.email, user.phone].filter(Boolean).join(' · ') || 'Họ tên, email, điện thoại', editProfile),
      item(user.hasPassword ? 'Đổi mật khẩu' : 'Đặt mật khẩu', null, changePassword),
      pushAvailable() ? item('Bật thông báo đẩy', 'Nhận thông báo kể cả khi đóng app (Firebase)', turnOnPush) : null,
      IS_NATIVE ? item('Địa chỉ máy chủ', session.serverUrl(), changeServer) : null),

    user.role === 'ADMIN' ? h('div', { class: 'stack-sm' },
      h('h2', { class: 'small muted', style: { fontSize: 'var(--text-sm)', textTransform: 'uppercase', letterSpacing: '0.04em' } }, 'Quản trị'),
      h('div', { class: 'list' },
        item('Người dùng', 'Phân quyền, khóa tài khoản', () => navigate('/admin/users')),
        item('Cài đặt quán & AI', 'Tên quán, giờ mở cửa, khóa API Trợ lý AI', () => navigate('/admin/settings')))) : null,

    h('button', { class: 'btn btn-danger btn-block', type: 'button', onclick: logout }, 'Đăng xuất'),
    h('p', { class: 'muted small', style: { textAlign: 'center' } }, 'AICF Coffee Mobile 1.0 · Đồ án Lập trình thiết bị di động'));
}
