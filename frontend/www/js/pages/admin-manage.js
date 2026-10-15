/**
 * QUẢN LÝ THỰC ĐƠN (ADMIN): 3 thẻ Món / Danh mục / Bàn.
 * Cả 3 đều dựng từ crudPage() - VÍ DỤ TÁI SỬ DỤNG phía app: mỗi màn chỉ cần mô tả trường form và cách hiển thị 1 dòng,
 * còn danh sách / thêm / sửa / xóa / xác nhận / báo lỗi đã có sẵn (giống CrudController + CrudService ở backend).
 */
import { h, mount } from '../core/dom.js';
import { money } from '../core/format.js';
import { categoryRepo, productRepo, tableRepo } from '../data/repositories.js';
import { setHeader } from '../ui/components.js';
import { crudPage } from '../ui/crud-page.js';

/** Cấu hình 3 tài nguyên. */
const RESOURCES = {
  products: () => crudPage({
    title: 'Món', singular: 'món', repo: productRepo, emoji: '☕',
    defaults: { available: true },
    // Trường "Danh mục" là ô chọn: danh sách lựa chọn tải từ máy chủ mỗi lần mở form
    fields: async () => [
      { name: 'name', label: 'Tên món', required: true },
      {
        name: 'categoryId', label: 'Danh mục', type: 'select', required: true,
        options: (await categoryRepo.list()).map((c) => ({ value: c.id, label: `${c.icon || ''} ${c.name}` })),
      },
      { name: 'price', label: 'Giá (đồng)', type: 'number', required: true, min: 0 },
      { name: 'description', label: 'Mô tả', type: 'textarea', hint: 'Trợ lý AI dùng mô tả này để tư vấn món' },
      { name: 'imageUrl', label: 'Link ảnh', type: 'text', hint: 'vd /img/latte.jpg hoặc https://...' },
      { name: 'available', label: 'Đang bán', type: 'checkbox' },
    ],
    item: (p) => ({
      title: p.name,
      sub: `${p.categoryName} · ${money(p.price)}`,
      thumb: p.imageUrl ? h('img', { src: p.imageUrl, alt: '', loading: 'lazy' }) : p.categoryIcon,
      meta: p.available ? null : h('span', { class: 'tag' }, 'Tạm hết'),
    }),
  }),

  categories: () => crudPage({
    title: 'Danh mục', singular: 'danh mục', repo: categoryRepo, emoji: '🗂️',
    defaults: { sortOrder: 0 },
    fields: [
      { name: 'name', label: 'Tên danh mục', required: true },
      { name: 'icon', label: 'Biểu tượng (emoji)', placeholder: '☕' },
      { name: 'description', label: 'Mô tả' },
      { name: 'sortOrder', label: 'Thứ tự hiển thị', type: 'number', hint: 'Số nhỏ hiện trước' },
    ],
    item: (c) => ({ title: c.name, sub: c.description, thumb: c.icon || '🗂️' }),
  }),

  tables: () => crudPage({
    title: 'Bàn', singular: 'bàn', repo: tableRepo, emoji: '🪑',
    defaults: { seats: 4, active: true },
    fields: [
      { name: 'name', label: 'Tên bàn', required: true, placeholder: 'Bàn 07' },
      { name: 'area', label: 'Khu vực', placeholder: 'Trong nhà / Sân vườn' },
      { name: 'seats', label: 'Số ghế', type: 'number', required: true, min: 1, max: 50 },
      { name: 'active', label: 'Đang sử dụng', type: 'checkbox' },
    ],
    item: (t) => ({ title: t.name, sub: `${t.area || 'Chưa đặt khu vực'} · ${t.seats} ghế`, thumb: '🪑', meta: t.active ? null : h('span', { class: 'tag' }, 'Tạm ngưng') }),
  }),
};

export function render(ctx) {
  setHeader('Quản lý');
  let current = RESOURCES[ctx.query.tab] ? ctx.query.tab : 'products';
  const box = h('div');
  const tabs = h('div', { class: 'segmented', role: 'radiogroup', 'aria-label': 'Chọn mục quản lý' },
    [['products', 'Món'], ['categories', 'Danh mục'], ['tables', 'Bàn']].map(([v, label]) =>
      h('label', null, h('input', {
        type: 'radio', name: 'manage-tab', value: v, checked: v === current,
        onchange: () => { current = v; mount(box, RESOURCES[v]()); },
      }), h('span', null, label))));
  mount(box, RESOURCES[current]());
  return h('div', { class: 'stack' }, tabs, box);
}
