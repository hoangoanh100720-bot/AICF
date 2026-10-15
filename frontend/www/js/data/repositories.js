/**
 * REPOSITORY - lớp duy nhất biết đường dẫn REST API của backend.
 * Màn hình gọi productRepo.list() thay vì tự viết fetch('/api/products') -> đổi API chỉ sửa 1 chỗ.
 *
 * createCrudRepository() là hàm TÁI SỬ DỤNG (tương ứng CrudController ở backend):
 * mọi tài nguyên CRUD chuẩn (danh mục, món, bàn) dùng chung 5 hàm list / get / create / update / remove.
 */
import { PLATFORM } from '../config.js';
import { api } from './api.js';
import { localDb } from './local-db.js';

/**
 * @param {string} resource  tên tài nguyên trên URL, vd 'categories' -> /api/categories
 */
export function createCrudRepository(resource) {
  const base = '/api/' + resource;
  return {
    list: (filters) => api.get(base, filters),          // GET    /api/{resource}?...
    get: (id) => api.get(`${base}/${id}`),              // GET    /api/{resource}/{id}
    create: (data) => api.post(base, data),             // POST   /api/{resource}
    update: (id, data) => api.put(`${base}/${id}`, data), // PUT  /api/{resource}/{id}
    remove: (id) => api.delete(`${base}/${id}`),        // DELETE /api/{resource}/{id}
  };
}

export const categoryRepo = createCrudRepository('categories');
export const tableRepo = createCrudRepository('tables');

/** Món: thêm khả năng xem thực đơn khi mất mạng (lưu bản sao vào LocalDb / SQLite). */
const productCrud = createCrudRepository('products');
export const productRepo = {
  ...productCrud,
  /**
   * Lấy thực đơn: có mạng thì lấy từ máy chủ và lưu bản sao; mất mạng thì trả bản sao.
   * @returns {Promise<{items: object[], offline: boolean}>}
   */
  async menu() {
    try {
      const items = await productCrud.list();
      localDb.cacheProducts(items).catch(() => { /* lưu thất bại không ảnh hưởng hiển thị */ });
      return { items, offline: false };
    } catch (e) {
      if (e.status !== 0) throw e;
      const cached = await localDb.cachedProducts();
      if (!cached.length) throw e;
      return { items: cached, offline: true };
    }
  },
};

export const authRepo = {
  login: (username, password) => api.post('/api/auth/login', { username, password }),
  register: (data) => api.post('/api/auth/register', data),
  firebase: (idToken) => api.post('/api/auth/firebase', { idToken }),
};

export const userRepo = {
  me: () => api.get('/api/users/me'),
  updateMe: (data) => api.put('/api/users/me', data),
  changePassword: (currentPassword, newPassword) => api.put('/api/users/me/password', { currentPassword, newPassword }),
  list: () => api.get('/api/users'),
  adminUpdate: (id, role, active) => api.put(`/api/users/${id}`, { role, active }),
};

export const orderRepo = {
  create: (data) => api.post('/api/orders', data),
  my: () => api.get('/api/orders/my'),
  get: (id) => api.get(`/api/orders/${id}`),
  cancel: (id, reason) => api.post(`/api/orders/${id}/cancel`, { reason }),
  active: () => api.get('/api/orders/active'),
  ofDay: (date) => api.get('/api/orders', { date }),
  setStatus: (id, status, cancelReason) => api.patch(`/api/orders/${id}/status`, { status, cancelReason }),
  markPaid: (id) => api.patch(`/api/orders/${id}/paid`),
};

export const chatRepo = {
  status: () => api.get('/api/chat/status'),
  history: (limit = 50) => api.get('/api/chat', { limit }),
  ask: (message) => api.post('/api/chat', { message }),
  clear: () => api.delete('/api/chat'),
};

export const notificationRepo = {
  list: () => api.get('/api/notifications'),
  unreadCount: () => api.get('/api/notifications/unread-count'),
  read: (id) => api.post(`/api/notifications/${id}/read`),
  readAll: () => api.post('/api/notifications/read-all'),
  registerDevice: (token) => api.post('/api/devices', { token, platform: PLATFORM }),
  unregisterDevice: (token) => api.delete('/api/devices', { token }),
};

export const statsRepo = {
  dashboard: (date) => api.get('/api/stats/dashboard', { date }),
};

export const settingsRepo = {
  health: () => api.get('/api/health'),
  shop: () => api.get('/api/shop'),
  updateShop: (data) => api.put('/api/settings/shop', data),
  ai: () => api.get('/api/settings/ai'),
  updateAi: (data) => api.put('/api/settings/ai', data),
};
