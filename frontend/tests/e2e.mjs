// =============================================================================
// KIỂM THỬ ĐẦU-CUỐI (end-to-end) GIAO DIỆN APP bằng Chrome chạy ẩn (headless), điều khiển qua
// Chrome DevTools Protocol - không cần cài thêm thư viện, chỉ cần Node.js 22+ và Google Chrome.
//
// Kịch bản: khách đăng nhập -> xem thực đơn, tìm món -> thêm vào giỏ (lưu ở LocalDb) -> đặt đơn tại bàn
// -> nhân viên xử lý đơn -> khách thấy trạng thái + thông báo -> chat AI -> admin xem tổng quan, quản lý món.
// Mỗi bước chụp ảnh màn hình vào thư mục kết quả để đưa vào báo cáo.
//
// Chạy (backend đang chạy với dữ liệu mẫu):
//   node tests/e2e.mjs [http://localhost:8090] [thu-muc-anh]
// =============================================================================
import { spawn } from 'node:child_process';
import { mkdirSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';

const BASE = process.argv[2] || 'http://localhost:8090';
const OUT = process.argv[3] || 'tests/ket-qua';
const CHROME = process.env.CHROME || 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe';
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
mkdirSync(OUT, { recursive: true });

// ---------- Mở Chrome ẩn + kết nối DevTools ----------
const port = 9400 + Math.floor(Math.random() * 50);
const chrome = spawn(CHROME, ['--headless=new', `--remote-debugging-port=${port}`, `--user-data-dir=${join(OUT, 'profile')}`,
  '--window-size=412,915', 'about:blank'], { stdio: 'ignore' });
process.on('exit', () => chrome.kill());
await sleep(2500);
const page = (await (await fetch(`http://127.0.0.1:${port}/json`)).json()).find((t) => t.type === 'page');
const ws = new WebSocket(page.webSocketDebuggerUrl);
await new Promise((r) => ws.addEventListener('open', r));
let seq = 0;
const pending = new Map();
const jsErrors = [];
ws.addEventListener('message', (e) => {
  const m = JSON.parse(e.data);
  if (m.id && pending.has(m.id)) { pending.get(m.id)(m); pending.delete(m.id); }
  if (m.method === 'Runtime.exceptionThrown') jsErrors.push(m.params.exceptionDetails.exception?.description || m.params.exceptionDetails.text);
});
const cdp = (method, params = {}) => new Promise((r) => { const id = ++seq; pending.set(id, r); ws.send(JSON.stringify({ id, method, params })); });
await cdp('Runtime.enable');
await cdp('Page.enable');
await cdp('Emulation.setDeviceMetricsOverride', { width: 412, height: 915, deviceScaleFactor: 1, mobile: true });

/** Chạy JS trong trang, trả kết quả. */
const ev = async (expr) => {
  const r = await cdp('Runtime.evaluate', { expression: expr, awaitPromise: true, returnByValue: true });
  if (r.result?.exceptionDetails) throw new Error(r.result.exceptionDetails.exception?.description || r.result.exceptionDetails.text);
  return r.result?.result?.value;
};
let shotNo = 0;
const shot = async (name) => {
  const r = await cdp('Page.captureScreenshot', { format: 'png' });
  writeFileSync(join(OUT, `${String(++shotNo).padStart(2, '0')}-${name}.png`), Buffer.from(r.result.data, 'base64'));
};
const results = [];
const ok = (name, cond, detail = '') => results.push({ name, ok: !!cond, detail: String(detail ?? '') });
const go = async (hash, wait = 1500) => { await ev(`location.hash = ${JSON.stringify(hash)}; true`); await sleep(wait); };
const text = () => ev('document.body.innerText');
const clickText = (sel, t) => ev(`(() => { const b = [...document.querySelectorAll(${JSON.stringify(sel)})].find((x) => x.textContent.includes(${JSON.stringify(t)})); if (b) b.click(); return !!b; })()`);
const fill = (sel, v) => ev(`(() => { const i = document.querySelector(${JSON.stringify(sel)}); i.value = ${JSON.stringify(v)}; i.dispatchEvent(new Event('input', {bubbles:true})); i.dispatchEvent(new Event('change', {bubbles:true})); return true; })()`);
const api = async (path, method = 'GET', body, token) => {
  const r = await fetch(BASE + path, { method, headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: 'Bearer ' + token } : {}) }, body: body ? JSON.stringify(body) : undefined });
  const t = await r.text();
  return { status: r.status, data: t ? JSON.parse(t) : null };
};
/** Đăng nhập bằng giao diện. */
const uiLogin = async (user, pass) => {
  await go('/login', 1200);
  await fill('input[name=username]', user);
  await fill('input[name=password]', pass);
  await ev(`document.querySelector('form button[type=submit]').click(); true`);
  await sleep(2000);
};
/** Đăng xuất bằng giao diện: Tài khoản -> Đăng xuất -> xác nhận. */
const uiLogout = async () => {
  await go('/profile', 1500);
  await clickText('.btn-danger', 'Đăng xuất');
  await sleep(700);
  await clickText('dialog .btn', 'Đăng xuất');
  await sleep(1500);
  ok('Đăng xuất -> về màn Đăng nhập', (await ev('location.hash')) === '#/login');
};

// =============================== KỊCH BẢN ===============================
await cdp('Page.navigate', { url: BASE + '/' });
await sleep(2500);
ok('Chưa đăng nhập -> chuyển tới màn Đăng nhập', (await ev('location.hash')) === '#/login');
await shot('dang-nhap');

// Sai mật khẩu
await fill('input[name=username]', 'khach');
await fill('input[name=password]', 'sai-mat-khau');
await ev(`document.querySelector('form button[type=submit]').click(); true`);
await sleep(1200);
ok('Sai mật khẩu -> báo lỗi', (await text()).includes('Sai tên đăng nhập hoặc mật khẩu'));

// --- KHÁCH ---
await uiLogin('khach', 'khach123');
ok('Khách đăng nhập -> vào Thực đơn', (await ev('location.hash')) === '#/menu');
ok('Thanh tab của khách có Giỏ hàng', await ev(`!!document.querySelector('.tab[href="#/cart"]')`));
const productCount = await ev(`document.querySelectorAll('.product').length`);
ok('Hiện danh sách món', productCount >= 10, productCount);
await shot('thuc-don');

await fill('#menu-search', 'latte');
await sleep(300);
ok('Tìm "latte" lọc đúng món', (await ev(`document.querySelectorAll('.product').length`)) === 1);
await fill('#menu-search', '');
await sleep(300);
await clickText('.chip', 'Trà');
await sleep(300);
ok('Lọc danh mục Trà', (await ev(`[...document.querySelectorAll('.product .name')].every(n => n.textContent.includes('Trà'))`)));
await clickText('.chip', 'Tất cả');
await sleep(300);

// Thêm 2 ly "Cà phê sữa đá" + 1 Latte
const addByName = (n) => ev(`(() => { const c = [...document.querySelectorAll('.product')].find(p => p.querySelector('.name').textContent === ${JSON.stringify(n)}); c.querySelector('.add-btn').click(); return true; })()`);
await addByName('Cà phê sữa đá'); await sleep(500);
await addByName('Cà phê sữa đá'); await sleep(500);
await addByName('Latte'); await sleep(800);
ok('Huy hiệu giỏ = 3', (await ev(`document.querySelector('.tab[href="#/cart"] .badge')?.textContent`)) === '3');
ok('Hiện nút giỏ hàng nổi', await ev(`!document.querySelector('.cart-fab').hidden`));
await shot('them-vao-gio');

// Giỏ còn sau khi tải lại trang (lưu trong LocalDb)
await cdp('Page.reload');
await sleep(2500);
ok('Giỏ hàng vẫn còn sau khi tải lại app (LocalDb)', (await ev(`document.querySelector('.tab[href="#/cart"] .badge')?.textContent`)) === '3');

await go('/cart', 1500);
await shot('gio-hang');
await ev(`document.querySelector('#table').value = document.querySelector('#table option:nth-child(3)').value; true`);
await fill('#order-note', 'Mang ra giúp mình nhé');
await ev(`document.querySelector('form button[type=submit]').click(); true`);
await sleep(2500);
const hash = await ev('location.hash');
ok('Đặt đơn -> mở chi tiết đơn', /^#\/orders\/\d+$/.test(hash), hash);
const orderId = Number(hash.split('/').pop());
const t1 = await text();
ok('Tổng tiền đúng 103.000 đ (2x29k + 45k)', t1.includes('103.000'));
ok('Trạng thái Chờ xác nhận', t1.includes('Chờ xác nhận'));
ok('Giỏ đã được làm trống', !(await ev(`document.querySelector('.tab[href="#/cart"] .badge')`)));
await shot('chi-tiet-don');

// --- NHÂN VIÊN xử lý đơn qua API (song song với khách) ---
const staff = (await api('/api/auth/login', 'POST', { username: 'staff', password: 'staff123' })).data.token;
for (const s of ['CONFIRMED', 'PREPARING', 'READY']) {
  const r = await api(`/api/orders/${orderId}/status`, 'PATCH', { status: s }, staff);
  ok(`Nhân viên chuyển đơn sang ${s}`, r.status === 200, r.status);
}
await cdp('Page.reload');
await sleep(2500);
ok('Khách thấy đơn "Đã xong, chờ lấy"', (await text()).includes('Đã xong, chờ lấy'));
await go('/profile', 1500);
await ev(`window.dispatchEvent(new Event('focus')); document.dispatchEvent(new Event('visibilitychange')); true`);
await sleep(1500);
ok('Có huy hiệu thông báo chưa đọc', !!(await ev(`document.querySelector('.tab[href="#/profile"] .badge')?.textContent`)));
await go('/notifications', 1500);
ok('Hộp thư có thông báo đơn', (await text()).includes('Đã xong, chờ lấy'));
await shot('thong-bao');

// Chat AI (chế độ cơ bản khi chưa có khóa API)
await go('/chat', 2000);
await fill('.composer input', 'Gợi ý cho mình 1 món');
await ev(`document.querySelector('.composer form').requestSubmit(); true`);
await sleep(2500);
ok('Chat AI trả lời', (await ev(`document.querySelectorAll('.msg-ai').length`)) >= 1);
await shot('chat-ai');

// Khách không vào được trang quản trị
await go('/admin', 1500);
ok('Khách bị chặn khỏi /admin', (await ev('location.hash')) === '#/menu');

// --- NHÂN VIÊN (giao diện) ---
await uiLogout();
await uiLogin('staff', 'staff123');
ok('Nhân viên đăng nhập -> Bảng đơn', (await ev('location.hash')) === '#/board');
ok('Bảng đơn có đơn vừa đặt', (await text()).includes('Đã giao & thu tiền'));
await shot('bang-don-nhan-vien');
// Bấm nút trên ĐÚNG thẻ của đơn vừa đặt (bảng có thể còn đơn khác)
await ev(`(() => { const card = document.querySelector('a[href="#/orders/${orderId}"]').closest('.board-card');
  [...card.querySelectorAll('.btn')].find(b => b.textContent.includes('Đã giao')).click(); return true; })()`);
await sleep(2000);
const done = await api(`/api/orders/${orderId}`, 'GET', null, staff);
ok('Bấm "Đã giao & thu tiền" -> đơn Hoàn thành', done.data.status === 'COMPLETED' && done.data.paid, done.data.status);

// --- ADMIN ---
await uiLogout();
await uiLogin('admin', 'admin123');
ok('Admin đăng nhập -> Tổng quan', (await ev('location.hash')) === '#/admin');
await sleep(1000);
const dash = (await api('/api/stats/dashboard', 'GET', null, staff)).status; // nhân viên không được xem
ok('Nhân viên gọi API thống kê bị từ chối 403', dash === 403, dash);
ok('Tổng quan hiện doanh thu và món bán chạy', (await text()).includes('Cà phê sữa đá'));
await shot('tong-quan-admin');
await go('/admin/manage', 2000);
ok('Quản lý món: có danh sách', (await ev(`document.querySelectorAll('.crud-item').length`)) >= 10);
await shot('quan-ly-mon');
// Thêm bàn mới bằng form tự sinh
await clickText('.segmented label', 'Bàn');
await sleep(1500);
await clickText('.btn', 'Thêm bàn');
await sleep(800);
await fill('dialog input[name=name]', 'Bàn E2E');
await fill('dialog input[name=seats]', '3');
await shot('form-them-ban');
await ev(`document.querySelector('dialog button[type=submit]').click(); true`);
await sleep(1500);
ok('Thêm bàn mới qua form CRUD tổng quát', (await text()).includes('Bàn E2E'));
// Xóa bàn vừa thêm
await ev(`(() => { const row = [...document.querySelectorAll('.crud-item')].find(r => r.textContent.includes('Bàn E2E')); row.querySelector('[aria-label^="Xóa"]').click(); return true; })()`);
await sleep(800);
await clickText('dialog .btn', 'Xóa');
await sleep(1500);
ok('Xóa bàn (có hộp xác nhận)', !(await text()).includes('Bàn E2E'));
await go('/admin/settings', 2000);
ok('Trang cài đặt AI', (await text()).includes('Trợ lý AI'));
await shot('cai-dat');

// =============================== KẾT QUẢ ===============================
console.log('== Kiểm thử giao diện AICF Coffee Mobile ==');
for (const r of results) console.log((r.ok ? 'OK   ' : 'FAIL ') + r.name + (r.detail ? `  [${r.detail}]` : ''));
console.log(jsErrors.length ? 'LỖI JS:\n' + jsErrors.join('\n') : 'Không có lỗi JS');
const failed = results.filter((r) => !r.ok).length + jsErrors.length;
console.log(`${results.length - results.filter((r) => !r.ok).length}/${results.length} bước đạt. Ảnh: ${OUT}`);
ws.close();
process.exit(failed ? 1 : 0);
