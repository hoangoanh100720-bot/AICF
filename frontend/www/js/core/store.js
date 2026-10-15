/**
 * KHO TRẠNG THÁI nhỏ theo mẫu Observer (publish / subscribe).
 * Nơi nào cần biết dữ liệu đổi (số món trong giỏ, số thông báo chưa đọc, người đang đăng nhập)
 * thì subscribe; nơi thay đổi gọi set() -> mọi nơi đăng ký đều được báo, giao diện tự cập nhật.
 *
 *   const s = createStore({ count: 0 });
 *   const off = s.subscribe(state => console.log(state.count));
 *   s.set({ count: 1 });   // in ra 1
 *   off();                 // hủy đăng ký
 */
export function createStore(initial) {
  let state = { ...initial };
  const listeners = new Set();
  return {
    get: () => state,
    set(patch) {
      state = { ...state, ...patch };
      listeners.forEach((fn) => fn(state));
    },
    subscribe(fn) {
      listeners.add(fn);
      return () => listeners.delete(fn);
    },
  };
}

/** Trạng thái chung của toàn app. */
export const appState = createStore({
  user: null,        // người đang đăng nhập (UserResponse từ backend) hoặc null
  cartCount: 0,      // tổng số ly trong giỏ
  unread: 0,         // số thông báo chưa đọc
  shop: null,        // thông tin quán từ GET /api/shop
  online: navigator.onLine,
});

window.addEventListener('online', () => appState.set({ online: true }));
window.addEventListener('offline', () => appState.set({ online: false }));
