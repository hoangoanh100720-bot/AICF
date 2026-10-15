/**
 * TRANG QUẢN LÝ CRUD TỔNG QUÁT - đối xứng với CrudController / CrudService ở backend.
 *
 * Chỉ cần mô tả tài nguyên, hàm này dựng sẵn: danh sách + nút Thêm + bấm 1 dòng để Sửa / Xóa,
 * có skeleton khi tải, trạng thái rỗng, lỗi + thử lại, xác nhận trước khi xóa.
 *
 *   crudPage({
 *     title: 'Danh mục', repo: categoryRepo, emoji: '🗂️',
 *     fields: [{ name: 'name', label: 'Tên', required: true }, ...],
 *     item: (c) => ({ title: c.name, sub: c.description, thumb: c.icon }),
 *   })
 * Dùng cho 3 màn: danh mục, món, bàn (xem pages/admin-manage.js).
 */
import { h } from '../core/dom.js';
import { asyncView, confirmDialog, emptyState, formSheet, toast, toastError } from './components.js';
import { icons } from './icons.js';

/**
 * @param {object} cfg
 * @param {string} cfg.title          tên tài nguyên (số nhiều), vd 'Món'
 * @param {string} cfg.singular       tên 1 bản ghi, vd 'món'
 * @param {object} cfg.repo           repository có list/create/update/remove (createCrudRepository)
 * @param {object[]|() => Promise<object[]>} cfg.fields  cấu hình trường form (có thể tải động, vd danh sách danh mục)
 * @param {(row) => {title, sub, thumb, meta}} cfg.item  cách hiển thị 1 dòng
 * @param {(row) => object} [cfg.toForm]  đổi dữ liệu dòng -> giá trị form khi sửa (mặc định giữ nguyên)
 * @param {string} [cfg.emoji]
 * @param {object} [cfg.defaults]     giá trị mặc định khi thêm mới
 */
export function crudPage(cfg) {
  const { title, singular, repo, item, toForm = (r) => r, emoji = '📋', defaults = {} } = cfg;
  const getFields = async () => (typeof cfg.fields === 'function' ? cfg.fields() : cfg.fields);

  const openForm = async (row, reload) => {
    let fields;
    try {
      fields = await getFields();
    } catch (err) {
      return toastError(err);
    }
    formSheet({
      title: row ? `Sửa ${singular}` : `Thêm ${singular}`,
      fields,
      initial: row ? toForm(row) : defaults,
      onSubmit: async (data) => {
        if (row) await repo.update(row.id, data);
        else await repo.create(data);
        toast(row ? 'Đã lưu thay đổi' : `Đã thêm ${singular}`, 'success');
        reload(true);
      },
    });
  };

  const remove = async (row, reload) => {
    const ok = await confirmDialog(`Xóa ${singular} "${item(row).title}"? Không thể hoàn tác.`, { okLabel: 'Xóa', danger: true });
    if (!ok) return;
    try {
      await repo.remove(row.id);
      toast(`Đã xóa ${singular}`, 'success');
      reload(true);
    } catch (err) {
      toastError(err); // vd 409: danh mục còn món
    }
  };

  const view = asyncView(() => repo.list(), (rows, reload) => {
    const addBtn = h('button', { class: 'btn', type: 'button', onclick: () => openForm(null, reload) },
      h('span', { html: icons.plus, style: { display: 'inline-flex', width: '20px' } }), `Thêm ${singular}`);
    if (!rows.length) return emptyState(emoji, `Chưa có ${singular} nào`, null, addBtn);
    return h('div', { class: 'stack' },
      h('div', { class: 'row-between' }, h('span', { class: 'muted small' }, `${rows.length} ${singular}`), addBtn),
      h('div', { class: 'list' }, rows.map((row) => {
        const v = item(row);
        return h('div', { class: 'list-item crud-item' },
          h('div', { class: 'thumb', 'aria-hidden': 'true' }, v.thumb ?? emoji),
          h('button', { class: 'unstyled-btn', type: 'button',
            onclick: () => openForm(row, reload), 'aria-label': `Sửa ${v.title}` },
          h('div', { class: 'title' }, v.title),
          v.sub ? h('div', { class: 'sub' }, v.sub) : null,
          v.meta ? h('div', { style: { marginTop: '4px' } }, v.meta) : null),
          h('button', { class: 'icon-btn', type: 'button', 'aria-label': `Xóa ${v.title}`, title: 'Xóa',
            html: icons.trash, onclick: () => remove(row, reload) }));
      })));
  });

  return h('section', { class: 'stack', 'aria-label': title }, view);
}

