/**
 * GIỎ HÀNG - lưu trong LocalDb (SQLite trên Android / localStorage trên PWA) nên vẫn còn khi tắt app, mất mạng.
 * Mỗi lần thay đổi đều cập nhật appState.cartCount để huy hiệu trên tab "Giỏ" tự đổi số.
 */
import { appState } from '../core/store.js';
import { localDb } from './local-db.js';

async function publish() {
  const items = await localDb.cartList();
  appState.set({ cartCount: items.reduce((n, i) => n + i.quantity, 0) });
  return items;
}

export const cart = {
  items: () => localDb.cartList(),

  /** Thêm 1 món (đã có thì cộng số lượng). */
  async add(product, quantity = 1) {
    const items = await localDb.cartList();
    const existing = items.find((i) => i.productId === product.id);
    await localDb.cartPut({
      productId: product.id,
      name: product.name,
      price: product.price,
      imageUrl: product.imageUrl,
      quantity: Math.min(99, (existing?.quantity || 0) + quantity),
      note: existing?.note ?? null,
    });
    return publish();
  },

  /** Đặt lại số lượng; về 0 thì xóa khỏi giỏ. */
  async setQuantity(productId, quantity) {
    const item = (await localDb.cartList()).find((i) => i.productId === productId);
    if (!item) return publish();
    if (quantity <= 0) await localDb.cartRemove(productId);
    else await localDb.cartPut({ ...item, quantity: Math.min(99, quantity) });
    return publish();
  },

  async setNote(productId, note) {
    const item = (await localDb.cartList()).find((i) => i.productId === productId);
    if (item) await localDb.cartPut({ ...item, note: note || null });
  },

  async clear() {
    await localDb.cartClear();
    return publish();
  },

  /** Tạm tính (giá thật do máy chủ tính lại khi đặt). */
  total: (items) => items.reduce((s, i) => s + i.price * i.quantity, 0),

  refresh: publish,
};
