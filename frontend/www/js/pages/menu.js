/**
 * MÀN HÌNH THỰC ĐƠN: lời chào, tìm kiếm, lọc theo danh mục, lưới món, thêm vào giỏ.
 * Dữ liệu: GET /api/categories + GET /api/products (mất mạng thì dùng bản sao trong SQLite / LocalDb).
 * Lọc và tìm làm ngay trên máy (thực đơn nhỏ) nên gõ tới đâu lọc tới đó, không phải gọi lại máy chủ.
 * KHÔNG cần đăng nhập: khách vãng lai xem món và thêm vào giỏ thoải mái, chỉ phải đăng nhập lúc bấm "Đặt món".
 */
import { h, mount } from '../core/dom.js';
import { money } from '../core/format.js';
import { appState } from '../core/store.js';
import { cart } from '../data/cart.js';
import { categoryRepo, productRepo } from '../data/repositories.js';
import { asyncView, emptyState, productThumb, setHeader, toast, toastError } from '../ui/components.js';
import { icons } from '../ui/icons.js';

export function render(ctx) {
  setHeader(appState.get().shop?.name || 'Thực đơn');
  const user = appState.get().user;
  // Khách (đã hoặc chưa đăng nhập) được thêm món vào giỏ; nhân viên / quản trị chỉ xem thực đơn
  const canOrder = !user || user.role === 'CUSTOMER';
  let category = ctx.query.category ? Number(ctx.query.category) : null;
  let keyword = '';

  const view = asyncView(
    async () => {
      const [menu, categories] = await Promise.all([productRepo.menu(), categoryRepo.list().catch(() => [])]);
      return { ...menu, categories };
    },
    ({ items, offline, categories }) => {
      const grid = h('div');
      const chips = h('div', { class: 'chips', role: 'toolbar', 'aria-label': 'Lọc theo danh mục' });

      const draw = () => {
        mount(chips, [{ id: null, name: 'Tất cả', icon: '' }, ...categories].map((c) =>
          h('button', {
            class: 'chip', type: 'button', 'aria-pressed': String(category === c.id),
            onclick: () => { category = c.id; draw(); },
          }, c.icon ? c.icon + ' ' : '', c.name)));
        const k = keyword.toLowerCase();
        const shown = items.filter((p) => (category === null || p.categoryId === category)
          && (!k || p.name.toLowerCase().includes(k) || (p.description || '').toLowerCase().includes(k)));
        mount(grid, shown.length
          ? h('div', { class: 'product-grid' }, shown.map((p) => productCard(p, canOrder)))
          : emptyState('🔍', 'Không tìm thấy món phù hợp', 'Thử từ khóa khác hoặc chọn "Tất cả".'));
      };

      const search = h('div', { class: 'search' },
        h('span', { html: icons.search }),
        h('label', { class: 'sr-only', for: 'menu-search' }, 'Tìm món'),
        h('input', {
          class: 'input', id: 'menu-search', type: 'search', placeholder: 'Tìm món: cà phê, trà đào...', value: keyword,
          oninput: (e) => { keyword = e.target.value.trim(); draw(); },
        }));
      draw();
      return h('div', { class: 'stack' },
        offline ? h('div', { class: 'banner banner-warn', role: 'status' }, '📡 Đang mất mạng - đây là thực đơn đã lưu, có thể chưa cập nhật.') : null,
        search, chips, grid);
    },
    { skeletonRows: 4, skeletonHeight: 120 });

  // Nút giỏ hàng nổi (chỉ khách), tự cập nhật theo số món
  const fab = h('a', { class: 'cart-fab', href: '#/cart', hidden: true });
  const updateFab = ({ cartCount }) => {
    fab.hidden = !canOrder || cartCount === 0;
    mount(fab, h('span', null, `🛒 ${cartCount} món trong giỏ`), h('span', null, 'Xem giỏ →'));
  };
  updateFab(appState.get());
  ctx.onLeave(appState.subscribe(updateFab));

  return h('div', { class: 'stack' },
    h('div', { class: 'shop-hero' },
      h('div', { class: 'grow' },
        h('p', { class: 'greet' }, greeting() + ','),
        h('h2', null, user ? user.fullName : 'Mời bạn chọn món')),
      appState.get().shop?.hours ? h('span', { class: 'tag' }, '🕒 ' + appState.get().shop.hours) : null),
    view, fab);
}

/** Thẻ 1 món - component dùng riêng cho màn này. */
function productCard(p, canOrder) {
  const add = async (e) => {
    e.currentTarget.disabled = true;
    try {
      await cart.add(p);
      toast(`Đã thêm ${p.name}`, 'success', 1500);
    } catch (err) {
      toastError(err);
    } finally {
      e.target.closest('button').disabled = false;
    }
  };
  return h('article', { class: 'product' + (p.available ? '' : ' sold-out') },
    h('div', { class: 'thumb' }, productThumb(p)),
    h('div', { class: 'body' },
      h('h3', { class: 'name', style: { fontSize: 'var(--text-base)' } }, p.name),
      p.description ? h('p', { class: 'desc' }, p.description) : null,
      h('div', { class: 'foot' },
        h('span', { class: 'price' }, money(p.price)),
        !p.available ? h('span', { class: 'tag' }, 'Tạm hết')
          : canOrder ? h('button', { class: 'add-btn', type: 'button', 'aria-label': `Thêm ${p.name} vào giỏ`, html: icons.plus, onclick: add })
            : null)));
}

function greeting() {
  const hour = new Date().getHours();
  return hour < 11 ? 'Chào buổi sáng' : hour < 14 ? 'Chào buổi trưa' : hour < 18 ? 'Chào buổi chiều' : 'Chào buổi tối';
}
