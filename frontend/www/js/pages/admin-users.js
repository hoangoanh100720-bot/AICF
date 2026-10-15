/**
 * QUẢN LÝ NGƯỜI DÙNG (ADMIN): GET /api/users, đổi vai trò / khóa bằng PUT /api/users/{id}.
 * Tài khoản mới do khách tự đăng ký (vai trò Khách hàng); Admin nâng lên Nhân viên khi tuyển người.
 */
import { h } from '../core/dom.js';
import { ROLE_LABEL } from '../core/format.js';
import { appState } from '../core/store.js';
import { userRepo } from '../data/repositories.js';
import { asyncView, emptyState, formSheet, iconButton, setHeader, toast } from '../ui/components.js';
import { icons } from '../ui/icons.js';

export function render() {
  setHeader('Người dùng', iconButton(icons.back, 'Quay lại', () => history.back()));
  const me = appState.get().user;
  return asyncView(() => userRepo.list(), (users, reload) => (users.length
    ? h('div', { class: 'list' }, users.map((u) => h('button', {
      class: 'list-item', type: 'button',
      onclick: () => formSheet({
        title: u.fullName,
        fields: [
          { name: 'role', label: 'Vai trò', type: 'select', required: true, options: Object.entries(ROLE_LABEL).map(([value, label]) => ({ value, label })) },
          { name: 'active', label: 'Cho phép đăng nhập', type: 'checkbox' },
        ],
        initial: u,
        onSubmit: async ({ role, active }) => {
          await userRepo.adminUpdate(u.id, role, active);
          toast('Đã cập nhật ' + u.fullName, 'success');
          reload(true);
        },
      }),
    },
    h('div', { class: 'grow' },
      h('div', { class: 'title' }, u.fullName, u.id === me.id ? ' (bạn)' : ''),
      h('div', { class: 'sub' }, '@' + u.username + (u.email ? ' · ' + u.email : ''))),
    u.active ? null : h('span', { class: 'tag' }, 'Đã khóa'),
    h('span', { class: 'tag' }, ROLE_LABEL[u.role]))))
    : emptyState('👥', 'Chưa có người dùng')));
}
