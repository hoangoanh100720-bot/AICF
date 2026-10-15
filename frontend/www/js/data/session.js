/**
 * PHIÊN ĐĂNG NHẬP: giữ JWT + người dùng + địa chỉ máy chủ, lưu xuống LocalDb để mở lại app không phải đăng nhập lại.
 * Đọc nhanh (đồng bộ) từ bộ nhớ; ghi thì ghi cả bộ nhớ lẫn LocalDb.
 */
import { DEFAULT_SERVER_URL } from '../config.js';
import { appState } from '../core/store.js';
import { localDb } from './local-db.js';

let token = null;
let serverUrl = DEFAULT_SERVER_URL;

export const session = {
  /** Nạp phiên đã lưu (gọi 1 lần lúc khởi động, sau initLocalDb). */
  async load() {
    token = await localDb.kvGet('session.token');
    serverUrl = (await localDb.kvGet('server_url')) ?? DEFAULT_SERVER_URL;
    appState.set({ user: token ? await localDb.kvGet('session.user') : null });
  },

  token: () => token,
  user: () => appState.get().user,
  serverUrl: () => serverUrl,

  /** Lưu sau khi đăng nhập / đăng ký thành công (dữ liệu AuthResponse từ backend). */
  async signIn(auth) {
    token = auth.token;
    await localDb.kvSet('session.token', auth.token);
    await this.setUser(auth.user);
  },

  /** Cập nhật thông tin người dùng (sau khi sửa hồ sơ). */
  async setUser(user) {
    await localDb.kvSet('session.user', user);
    appState.set({ user });
  },

  async signOut() {
    token = null;
    await localDb.kvRemove('session.token');
    await localDb.kvRemove('session.user');
    appState.set({ user: null, unread: 0 });
  },

  /** Đổi địa chỉ máy chủ (app Android). Bỏ dấu / cuối. */
  async setServerUrl(url) {
    serverUrl = (url || '').trim().replace(/\/+$/, '');
    await localDb.kvSet('server_url', serverUrl);
  },
};
