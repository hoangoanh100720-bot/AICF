/**
 * HTTP CLIENT dùng chung cho mọi lời gọi REST API.
 *
 * Gom 1 chỗ các việc lặp lại: ghép địa chỉ máy chủ, gắn header "Authorization: Bearer <JWT>",
 * đổi body sang JSON, đọc lỗi JSON của backend ({status, message, details}) thành ApiError,
 * và xử lý mất mạng / hết phiên đăng nhập. Các repository chỉ việc gọi api.get('/api/...').
 */

/** Lỗi khi gọi API. status = 0 nghĩa là không kết nối được máy chủ. */
export class ApiError extends Error {
  constructor(status, message, details = []) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.details = details;
  }
}

export class HttpClient {
  /**
   * @param {object} options
   * @param {() => string} options.baseUrl         hàm trả địa chỉ máy chủ hiện tại ('' = cùng nguồn)
   * @param {() => string|null} options.getToken   hàm trả JWT hiện tại
   * @param {() => void} [options.onUnauthorized]  gọi khi máy chủ trả 401 (token hết hạn) để đăng xuất
   * @param {number} [options.timeoutMs]           quá thời gian thì hủy request
   */
  constructor({ baseUrl, getToken, onUnauthorized, timeoutMs = 30_000 }) {
    this.baseUrl = baseUrl;
    this.getToken = getToken;
    this.onUnauthorized = onUnauthorized;
    this.timeoutMs = timeoutMs;
  }

  /**
   * Gửi 1 request.
   * @param {'GET'|'POST'|'PUT'|'PATCH'|'DELETE'} method
   * @param {string} path      vd '/api/products'
   * @param {object} [body]    đối tượng sẽ đổi sang JSON
   * @param {object} [query]   tham số trên URL, vd { categoryId: 1 }
   * @returns {Promise<any>}   dữ liệu JSON, hoặc null nếu 204 No Content
   */
  async request(method, path, body, query) {
    const url = new URL(this.baseUrl() + path, window.location.href);
    for (const [k, v] of Object.entries(query || {})) {
      if (v !== undefined && v !== null && v !== '') url.searchParams.set(k, v);
    }
    const headers = { Accept: 'application/json' };
    if (body !== undefined) headers['Content-Type'] = 'application/json';
    const token = this.getToken();
    if (token) headers.Authorization = 'Bearer ' + token;

    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), this.timeoutMs);
    let res;
    try {
      res = await fetch(url, {
        method,
        headers,
        body: body === undefined ? undefined : JSON.stringify(body),
        signal: controller.signal,
      });
    } catch (e) {
      throw new ApiError(0, e.name === 'AbortError'
        ? 'Máy chủ phản hồi quá lâu, vui lòng thử lại'
        : 'Không kết nối được máy chủ. Kiểm tra mạng hoặc địa chỉ máy chủ.');
    } finally {
      clearTimeout(timer);
    }

    if (res.status === 204) return null;
    const data = await res.json().catch(() => null);
    if (!res.ok) {
      if (res.status === 401 && token && this.onUnauthorized) this.onUnauthorized();
      throw new ApiError(res.status, (data && data.message) || `Lỗi ${res.status}`, (data && data.details) || []);
    }
    return data;
  }

  get(path, query) { return this.request('GET', path, undefined, query); }
  post(path, body) { return this.request('POST', path, body ?? {}); }
  put(path, body) { return this.request('PUT', path, body ?? {}); }
  patch(path, body) { return this.request('PATCH', path, body ?? {}); }
  delete(path, query) { return this.request('DELETE', path, undefined, query); }
}
