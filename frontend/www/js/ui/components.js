/**
 * COMPONENT GIAO DIỆN TÁI SỬ DỤNG - mọi màn hình dùng chung, đảm bảo giao diện đồng nhất:
 *   toast()          thông báo nổi ngắn
 *   openSheet()      hộp thoại trượt từ dưới lên (dùng thẻ <dialog> gốc: tự khóa focus, Esc để đóng)
 *   confirmDialog()  hỏi Có / Không, trả Promise<boolean>
 *   formSheet()      FORM TỰ SINH từ danh sách trường (dùng cho mọi màn thêm / sửa)
 *   asyncView()      vùng "đang tải -> có dữ liệu | rỗng | lỗi + nút thử lại"
 *   emptyState(), errorState(), skeleton(), statusChip(), productThumb(), setHeader()
 */
import { h, mount } from '../core/dom.js';
import { STATUS_LABEL } from '../core/format.js';
import { icons } from './icons.js';

/* ------------------------------ Thanh tiêu đề ------------------------------ */

/** Đổi tiêu đề và các nút bên phải thanh tiêu đề của màn hình hiện tại. */
export function setHeader(title, ...actions) {
  document.getElementById('page-title').textContent = title;
  mount(document.getElementById('header-actions'), ...actions);
}

/** Nút biểu tượng (có nhãn cho trình đọc màn hình). */
export const iconButton = (icon, label, onclick) =>
  h('button', { class: 'icon-btn', type: 'button', 'aria-label': label, title: label, onclick, html: icon });

/* ------------------------------ Toast ------------------------------ */

/** @param {'info'|'success'|'error'} type */
export function toast(message, type = 'info', ms = 3000) {
  const el = h('div', { class: 'toast' + (type === 'info' ? '' : ' toast-' + type) }, message);
  document.getElementById('toasts').appendChild(el);
  setTimeout(() => el.remove(), ms);
}

/** Hiện lỗi API (hoặc lỗi bất kỳ) bằng toast đỏ. */
export const toastError = (err) => toast(err?.message || 'Có lỗi xảy ra', 'error', 4500);

/* ------------------------------ Hộp thoại ------------------------------ */

/**
 * Mở hộp thoại dạng "bottom sheet".
 * @param {object} opts
 * @param {string} opts.title
 * @param {Node} opts.body
 * @param {Node[]} [opts.footer]  các nút cuối hộp thoại
 * @returns {{dialog: HTMLDialogElement, close: () => void}}
 */
export function openSheet({ title, body, footer = [] }) {
  const dialog = h('dialog', { class: 'sheet', 'aria-label': title },
    h('div', { class: 'sheet-head' },
      h('h2', { style: { fontSize: 'var(--text-lg)' } }, title),
      iconButton(icons.close, 'Đóng', () => close())),
    h('div', { class: 'sheet-body' }, body),
    footer.length ? h('div', { class: 'sheet-foot' }, footer) : null);
  const opener = document.activeElement;
  const close = () => dialog.close();
  dialog.addEventListener('close', () => {
    dialog.remove();
    opener?.focus?.(); // trả focus về nút đã mở hộp thoại
  });
  // Bấm ra nền mờ bên ngoài thì đóng
  dialog.addEventListener('click', (e) => { if (e.target === dialog) close(); });
  document.body.appendChild(dialog);
  dialog.showModal();
  return { dialog, close };
}

/** Hỏi xác nhận. @returns {Promise<boolean>} */
export function confirmDialog(message, { title = 'Xác nhận', okLabel = 'Đồng ý', danger = false } = {}) {
  return new Promise((resolve) => {
    let answered = false;
    const answer = (v) => { answered = true; resolve(v); sheet.close(); };
    const sheet = openSheet({
      title,
      body: h('p', null, message),
      footer: [
        h('button', { class: 'btn btn-secondary', type: 'button', onclick: () => answer(false) }, 'Hủy'),
        h('button', { class: 'btn' + (danger ? ' btn-danger' : ''), type: 'button', onclick: () => answer(true) }, okLabel),
      ],
    });
    sheet.dialog.addEventListener('close', () => { if (!answered) resolve(false); });
  });
}

/* ------------------------------ Form tự sinh ------------------------------ */

/**
 * Tạo 1 ô nhập theo cấu hình. Các kiểu: text | password | email | number | url | textarea | select | checkbox.
 * @param {object} f  { name, label, type, required, min, max, placeholder, hint, options: [{value,label}] }
 * @param {any} value giá trị ban đầu
 */
export function field(f, value) {
  const id = 'f-' + f.name + '-' + Math.random().toString(36).slice(2, 7);
  if (f.type === 'checkbox') {
    return h('label', { class: 'checkbox', dataset: { field: f.name } },
      h('input', { type: 'checkbox', id, name: f.name, checked: !!value }), f.label);
  }
  let input;
  const common = { id, name: f.name, class: 'input', required: !!f.required, placeholder: f.placeholder || null };
  if (f.type === 'textarea') {
    input = h('textarea', common, value ?? '');
  } else if (f.type === 'select') {
    input = h('select', common,
      !f.required ? h('option', { value: '' }, '— Chọn —') : null,
      (f.options || []).map((o) => h('option', { value: o.value, selected: String(o.value) === String(value ?? '') }, o.label)));
  } else {
    input = h('input', {
      ...common, type: f.type || 'text', value: value ?? '',
      min: f.min ?? null, max: f.max ?? null, minlength: f.minLength ?? null,
      autocomplete: f.autocomplete || null, inputmode: f.type === 'number' ? 'numeric' : null,
    });
  }
  return h('div', { class: 'field', dataset: { field: f.name } },
    h('label', { for: id }, f.label, f.required ? ' *' : ''),
    input,
    f.hint ? h('span', { class: 'hint' }, f.hint) : null,
    h('span', { class: 'error', role: 'alert' }));
}

/** Đọc giá trị form theo cấu hình trường (đổi number / checkbox về đúng kiểu). */
export function readForm(form, fields) {
  const out = {};
  for (const f of fields) {
    const el = form.elements[f.name];
    if (!el) continue;
    if (f.type === 'checkbox') out[f.name] = el.checked;
    else if (f.type === 'number') out[f.name] = el.value === '' ? null : Number(el.value);
    else out[f.name] = el.value.trim() === '' ? null : el.value.trim();
  }
  return out;
}

/** Hiện lỗi trả về từ backend ("name: không được để trống") ngay dưới ô tương ứng. */
export function showFieldErrors(form, err) {
  form.querySelectorAll('.field .error').forEach((e) => { e.textContent = ''; });
  form.querySelectorAll('[aria-invalid]').forEach((e) => e.removeAttribute('aria-invalid'));
  let shown = false;
  for (const d of err?.details || []) {
    const [name, ...rest] = d.split(':');
    const box = form.querySelector(`[data-field="${name.trim()}"]`);
    if (box) {
      box.querySelector('.error') && (box.querySelector('.error').textContent = rest.join(':').trim());
      box.querySelector('.input')?.setAttribute('aria-invalid', 'true');
      shown = true;
    }
  }
  return shown;
}

/**
 * HỘP THOẠI FORM TỰ SINH - viết 1 lần, dùng cho mọi màn thêm/sửa (danh mục, món, bàn, hồ sơ, cài đặt...).
 * @param {object} opts
 * @param {string} opts.title
 * @param {object[]} opts.fields      cấu hình trường (xem field())
 * @param {object} [opts.initial]     giá trị ban đầu (khi sửa)
 * @param {string} [opts.submitLabel]
 * @param {(data) => Promise<any>} opts.onSubmit  gửi lên máy chủ; ném lỗi thì form hiện lỗi, không đóng
 */
export function formSheet({ title, fields, initial = {}, submitLabel = 'Lưu', onSubmit }) {
  const formId = 'form-' + Math.random().toString(36).slice(2, 8);
  const submit = h('button', { class: 'btn', type: 'submit', form: formId }, submitLabel);
  const form = h('form', { id: formId, class: 'stack', novalidate: true },
    fields.map((f) => field(f, initial[f.name] ?? f.default)));
  const sheet = openSheet({
    title, body: form,
    footer: [h('button', { class: 'btn btn-secondary', type: 'button', onclick: () => sheet.close() }, 'Hủy'), submit],
  });
  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    if (!form.reportValidity()) return; // kiểm tra required/min/max ngay trên máy trước khi gửi
    submit.disabled = true;
    try {
      await onSubmit(readForm(form, fields));
      sheet.close();
    } catch (err) {
      if (!showFieldErrors(form, err)) toastError(err);
    } finally {
      submit.disabled = false;
    }
  });
  form.querySelector('input, select, textarea')?.focus();
  return sheet;
}

/* ------------------------------ Trạng thái hiển thị ------------------------------ */

export const emptyState = (emoji, title, text, action) =>
  h('div', { class: 'empty' }, h('div', { class: 'emoji', 'aria-hidden': 'true' }, emoji),
    h('strong', null, title), text ? h('p', null, text) : null, action || null);

export const errorState = (err, retry) =>
  emptyState(err?.status === 0 ? '📡' : '⚠️',
    err?.status === 0 ? 'Mất kết nối' : 'Không tải được dữ liệu',
    err?.message,
    retry ? h('button', { class: 'btn btn-secondary', type: 'button', onclick: retry }, 'Thử lại') : null);

/** Khung xương (skeleton) đúng hình dạng nội dung sắp hiện, thay cho vòng quay. */
export const skeleton = (n = 3, height = 72) =>
  h('div', { class: 'stack-sm', 'aria-busy': 'true', 'aria-label': 'Đang tải' },
    Array.from({ length: n }, () => h('div', { class: 'skeleton', style: { height: height + 'px' } })));

/**
 * VÙNG DỮ LIỆU BẤT ĐỒNG BỘ dùng lại ở mọi màn hình:
 * hiện skeleton -> gọi loader() -> vẽ bằng render(data) | lỗi + nút "Thử lại".
 * Trả về phần tử có thêm hàm .reload() để làm mới.
 */
export function asyncView(loader, render, { skeletonRows = 3, skeletonHeight = 72 } = {}) {
  const box = h('div');
  const load = async (silent = false) => {
    if (!silent) mount(box, skeleton(skeletonRows, skeletonHeight));
    try {
      mount(box, await render(await loader(), load));
    } catch (err) {
      console.error(err);
      if (!silent) mount(box, errorState(err, () => load()));
      else toastError(err);
    }
  };
  box.reload = load;
  load();
  return box;
}

/* ------------------------------ Phần tử nhỏ ------------------------------ */

export const statusChip = (status) => h('span', { class: 'status status-' + status }, STATUS_LABEL[status] || status);

/** Ảnh món, không có ảnh thì hiện biểu tượng danh mục. */
export function productThumb(p) {
  return p.imageUrl
    ? h('img', { src: p.imageUrl, alt: '', loading: 'lazy', onerror: (e) => e.target.replaceWith(h('span', null, p.categoryIcon || '☕')) })
    : h('span', { 'aria-hidden': 'true' }, p.categoryIcon || '☕');
}
