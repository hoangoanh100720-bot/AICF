/** ĐỊNH DẠNG hiển thị dùng chung: tiền, ngày giờ, nhãn trạng thái. */

const moneyFmt = new Intl.NumberFormat('vi-VN');

/** 29000 -> "29.000 đ" */
export const money = (v) => moneyFmt.format(Number(v) || 0) + ' đ';

/** "2026-10-15T09:30:00" -> "09:30 15/10" (hôm nay thì chỉ giờ) */
export function dateTime(iso) {
  if (!iso) return '';
  const d = new Date(iso);
  const hm = d.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
  const today = new Date();
  if (d.toDateString() === today.toDateString()) return hm;
  return hm + ' ' + d.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' });
}

/** Ngày hôm nay dạng YYYY-MM-DD theo giờ máy (dùng cho tham số API). */
export function todayIso() {
  const d = new Date();
  return new Date(d.getTime() - d.getTimezoneOffset() * 60_000).toISOString().slice(0, 10);
}

/** Thứ tự trạng thái đơn để vẽ thanh tiến trình. */
export const ORDER_FLOW = ['PENDING', 'CONFIRMED', 'PREPARING', 'READY', 'COMPLETED'];

export const STATUS_LABEL = {
  PENDING: 'Chờ xác nhận',
  CONFIRMED: 'Đã xác nhận',
  PREPARING: 'Đang pha chế',
  READY: 'Đã xong, chờ lấy',
  COMPLETED: 'Hoàn thành',
  CANCELLED: 'Đã hủy',
};

/** Bước tiếp theo nhân viên bấm cho từng trạng thái (khớp luật OrderStatus phía backend). */
export const NEXT_STATUS = {
  PENDING: { status: 'CONFIRMED', label: 'Nhận đơn' },
  CONFIRMED: { status: 'PREPARING', label: 'Bắt đầu pha' },
  PREPARING: { status: 'READY', label: 'Đã xong' },
  READY: { status: 'COMPLETED', label: 'Đã giao & thu tiền' },
};

export const PAYMENT_LABEL = { CASH: 'Tiền mặt', BANK_TRANSFER: 'Chuyển khoản', E_WALLET: 'Ví điện tử' };
export const ROLE_LABEL = { ADMIN: 'Quản trị', STAFF: 'Nhân viên', CUSTOMER: 'Khách hàng' };
