/**
 * MÀN HÌNH ĐĂNG NHẬP / ĐĂNG KÝ.
 *  - Đăng nhập bằng tên + mật khẩu  -> POST /api/auth/login
 *  - Đăng ký tài khoản khách          -> POST /api/auth/register
 *  - Đăng nhập Google (Firebase)      -> Firebase Auth -> POST /api/auth/firebase
 *  - Khách vãng lai được đưa tới đây khi bấm "Đặt món" / xem đơn / chat: tham số ?next=/cart cho biết
 *    đăng nhập xong phải quay lại đâu (giỏ hàng vẫn còn nguyên vì lưu trong máy).
 *  - App Android: nhập địa chỉ máy chủ, bấm "Kiểm tra" gọi GET /api/health
 */
import { IS_NATIVE } from '../config.js';
import { h, mount } from '../core/dom.js';
import { navigate } from '../core/router.js';
import { authRepo, settingsRepo } from '../data/repositories.js';
import { session } from '../data/session.js';
import { canUseGoogleSignIn, googleSignIn } from '../firebase/firebase.js';
import { field, iconButton, readForm, setHeader, showFieldErrors, toast, toastError } from '../ui/components.js';
import { icons } from '../ui/icons.js';

const LOGIN_FIELDS = [
  { name: 'username', label: 'Tên đăng nhập', required: true, autocomplete: 'username' },
  { name: 'password', label: 'Mật khẩu', type: 'password', required: true, autocomplete: 'current-password' },
];

const REGISTER_FIELDS = [
  { name: 'fullName', label: 'Họ tên', required: true, autocomplete: 'name' },
  { name: 'username', label: 'Tên đăng nhập', required: true, minLength: 3, autocomplete: 'username', hint: 'Chữ không dấu, số, dấu . _ -' },
  { name: 'password', label: 'Mật khẩu', type: 'password', required: true, minLength: 6, autocomplete: 'new-password', hint: 'Ít nhất 6 ký tự' },
  { name: 'email', label: 'Email', type: 'email', autocomplete: 'email' },
  { name: 'phone', label: 'Số điện thoại', type: 'tel', autocomplete: 'tel' },
];

export function render(ctx) {
  // Chỉ nhận đường dẫn nội bộ của app (bắt đầu bằng "/") để tránh bị chèn địa chỉ lạ
  const next = (ctx.query.next || '').startsWith('/') ? ctx.query.next : null;
  setHeader(next ? 'Đăng nhập' : 'Chào mừng',
    iconButton(icons.back, 'Quay lại', () => navigate(next === '/cart' ? '/cart' : '/menu')));
  let mode = 'login';
  const formBox = h('div');

  /**
   * Đăng nhập xong: lưu phiên rồi quay lại màn khách đang làm dở (vd giỏ hàng để đặt món);
   * nhân viên / quản trị luôn về trang chủ theo vai trò của mình.
   */
  const finish = async (auth) => {
    await session.signIn(auth);
    toast(`Xin chào, ${auth.user.fullName}!`, 'success');
    navigate(auth.user.role === 'CUSTOMER' && next ? next : '/');
  };

  // Lý do phải đăng nhập (khi khách bị chuyển tới từ màn khác)
  const reason = !next ? null
    : next === '/cart' ? 'Đăng nhập hoặc tạo tài khoản để đặt món. Giỏ hàng của bạn vẫn được giữ nguyên.'
      : next === '/chat' ? 'Đăng nhập để trò chuyện với Trợ lý AI.'
        : 'Bạn cần đăng nhập để xem mục này.';

  const drawForm = () => {
    const fields = mode === 'login' ? LOGIN_FIELDS : REGISTER_FIELDS;
    const submit = h('button', { class: 'btn btn-block', type: 'submit' }, mode === 'login' ? 'Đăng nhập' : 'Tạo tài khoản');
    const form = h('form', { class: 'stack', novalidate: true }, fields.map((f) => field(f)), submit);
    form.addEventListener('submit', async (e) => {
      e.preventDefault();
      if (!form.reportValidity()) return;
      submit.disabled = true;
      try {
        const data = readForm(form, fields);
        await finish(mode === 'login' ? await authRepo.login(data.username, data.password) : await authRepo.register(data));
      } catch (err) {
        if (!showFieldErrors(form, err)) toastError(err);
      } finally {
        submit.disabled = false;
      }
    });
    mount(formBox, form);
  };

  const modeSwitch = h('div', { class: 'segmented', role: 'radiogroup', 'aria-label': 'Chọn đăng nhập hoặc đăng ký' },
    [['login', 'Đăng nhập'], ['register', 'Đăng ký']].map(([value, label]) =>
      h('label', null,
        h('input', { type: 'radio', name: 'mode', value, checked: value === mode, onchange: () => { mode = value; drawForm(); } }),
        h('span', null, label))));
  drawForm();

  // Đăng nhập Google qua Firebase (chỉ hiện khi đã cấu hình Firebase và đang chạy trên trình duyệt)
  const google = canUseGoogleSignIn()
    ? [h('div', { class: 'divider' }, 'hoặc'),
      h('button', {
        class: 'btn btn-secondary btn-block', type: 'button',
        onclick: async (e) => {
          e.currentTarget.disabled = true;
          try {
            await finish(await authRepo.firebase(await googleSignIn()));
          } catch (err) {
            if (err?.code !== 'auth/popup-closed-by-user') toastError(err);
          } finally {
            e.target.closest('button').disabled = false;
          }
        },
      }, h('span', { 'aria-hidden': 'true', style: { fontWeight: 800, color: 'var(--info)' } }, 'G'), 'Tiếp tục với Google')]
    : null;

  return h('div', { class: 'stack' },
    h('div', { class: 'auth-hero' }, h('img', { src: 'img/hero-1.jpg', alt: 'Ly cà phê espresso đang được rót' })),
    h('div', { class: 'card auth-card stack' },
      h('div', null, h('h1', null, 'AICF Coffee'), h('p', { class: 'muted' }, 'Đặt món nhanh, theo dõi đơn và hỏi Trợ lý AI.')),
      reason ? h('div', { class: 'banner banner-info', role: 'status' }, reason) : null,
      modeSwitch, formBox, google),
    next === '/cart'
      ? h('a', { class: 'btn btn-ghost', href: '#/cart' }, '← Quay lại giỏ hàng')
      : h('a', { class: 'btn btn-ghost', href: '#/menu' }, '← Xem thực đơn không cần đăng nhập'),
    IS_NATIVE ? serverSettings() : null,
    h('p', { class: 'muted small', style: { textAlign: 'center' } },
      'Tài khoản thử: khach / khach123 · staff / staff123 · admin / admin123'));
}

/** App Android: chỉnh địa chỉ máy chủ (máy ảo: http://10.0.2.2:8090, điện thoại thật: IP máy tính trong cùng Wi-Fi). */
function serverSettings() {
  const input = h('input', { class: 'input', id: 'server-url', type: 'url', value: session.serverUrl(), inputmode: 'url' });
  const check = h('button', {
    class: 'btn btn-secondary', type: 'button',
    onclick: async () => {
      await session.setServerUrl(input.value);
      try {
        await settingsRepo.health();
        toast('Kết nối máy chủ thành công', 'success');
      } catch (err) {
        toastError(err);
      }
    },
  }, 'Kiểm tra');
  return h('details', { class: 'card' },
    h('summary', { style: { cursor: 'pointer', fontWeight: 600 } }, 'Địa chỉ máy chủ'),
    h('div', { class: 'field', style: { marginTop: 'var(--space-3)' } },
      h('label', { for: 'server-url' }, 'URL máy chủ'),
      h('div', { class: 'row' }, input, check),
      h('span', { class: 'hint' }, 'Máy ảo Android: http://10.0.2.2:8090 · Điện thoại thật: http://<IP máy tính>:8090')));
}
