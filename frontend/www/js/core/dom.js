/**
 * TIỆN ÍCH TẠO GIAO DIỆN BẰNG JS (không dùng framework).
 *
 * Hàm h() tạo phần tử DOM gọn như JSX:
 *   h('button', { class: 'btn', onclick: save }, 'Lưu')
 *   h('ul', null, items.map(i => h('li', null, i.name)))
 *
 * An toàn trước XSS: chữ luôn được thêm bằng document.createTextNode, KHÔNG dùng innerHTML với dữ liệu người dùng
 * (tên món, tin nhắn chat... có thể chứa ký tự < > nguy hiểm).
 */

/**
 * @param {string} tag      tên thẻ
 * @param {object|null} props  thuộc tính: class, style, dataset, on<Sự kiện> (hàm), hoặc thuộc tính HTML bất kỳ
 * @param {...any} children    chữ, số, Node, mảng (lồng nhau được), null/false bị bỏ qua
 * @returns {HTMLElement}
 */
export function h(tag, props, ...children) {
  const el = document.createElement(tag);
  for (const [key, value] of Object.entries(props || {})) {
    if (value === null || value === undefined || value === false) continue;
    if (key.startsWith('on') && typeof value === 'function') {
      el.addEventListener(key.slice(2).toLowerCase(), value);
    } else if (key === 'class') {
      el.className = value;
    } else if (key === 'style' && typeof value === 'object') {
      Object.assign(el.style, value);
    } else if (key === 'dataset') {
      Object.assign(el.dataset, value);
    } else if (key === 'html') {
      el.innerHTML = value; // CHỈ dùng cho nội dung tĩnh do lập trình viên viết (vd. biểu tượng SVG)
    } else if (key in el && typeof value !== 'string') {
      el[key] = value;    // vd. checked, disabled, value là thuộc tính DOM
    } else {
      el.setAttribute(key, value === true ? '' : String(value));
    }
  }
  append(el, children);
  return el;
}

function append(parent, children) {
  for (const c of children) {
    if (c === null || c === undefined || c === false) continue;
    if (Array.isArray(c)) append(parent, c);
    else parent.appendChild(c instanceof Node ? c : document.createTextNode(String(c)));
  }
}

/** Xóa hết con rồi thêm nội dung mới. */
export function mount(parent, ...children) {
  parent.replaceChildren();
  append(parent, children);
  return parent;
}

/** Tìm phần tử theo CSS selector. */
export const $ = (selector, root = document) => root.querySelector(selector);
