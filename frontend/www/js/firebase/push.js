/**
 * THÔNG BÁO ĐẨY (Firebase Cloud Messaging) - 1 hàm dùng chung cho cả 2 môi trường:
 *  - App Android: plugin @capacitor/push-notifications (FCM gốc của Android).
 *  - PWA: Firebase JS SDK + service worker (Web Push).
 * Lấy được mã thiết bị thì gửi lên backend (POST /api/devices) để backend biết gửi thông báo tới đâu.
 */
import { IS_NATIVE } from '../config.js';
import { localDb } from '../data/local-db.js';
import { notificationRepo } from '../data/repositories.js';
import { toast } from '../ui/components.js';
import { getWebPushToken, isFirebaseConfigured, onForegroundMessage } from './firebase.js';
import { NATIVE_PUSH_ENABLED, VAPID_KEY } from './firebase-config.js';

/** Thiết bị này có thể bật thông báo đẩy không (đủ cấu hình + trình duyệt hỗ trợ). */
export function pushAvailable() {
  if (IS_NATIVE) return NATIVE_PUSH_ENABLED && !!window.Capacitor?.Plugins?.PushNotifications;
  return isFirebaseConfigured() && !!VAPID_KEY && 'serviceWorker' in navigator && 'Notification' in window;
}

/**
 * Xin quyền + đăng ký nhận thông báo. Trên trình duyệt PHẢI gọi từ thao tác của người dùng (bấm nút).
 * @param {(orderId: string) => void} onOpenOrder  bấm vào thông báo thì mở đơn
 * @returns {Promise<boolean>} true nếu bật thành công
 */
export async function enablePush(onOpenOrder) {
  if (!pushAvailable()) return false;
  return IS_NATIVE ? enableNative(onOpenOrder) : enableWeb();
}

/** Gỡ mã thiết bị khỏi backend khi đăng xuất (để không nhận thông báo của tài khoản cũ). */
export async function disablePush() {
  const token = await localDb.kvGet('push.token');
  if (token) {
    await notificationRepo.unregisterDevice(token).catch(() => {});
    await localDb.kvRemove('push.token');
  }
}

async function saveToken(token) {
  await notificationRepo.registerDevice(token);
  await localDb.kvSet('push.token', token);
}

/* ---------- App Android ---------- */
async function enableNative(onOpenOrder) {
  const Push = window.Capacitor.Plugins.PushNotifications;
  let perm = await Push.checkPermissions();
  if (perm.receive === 'prompt' || perm.receive === 'prompt-with-rationale') perm = await Push.requestPermissions();
  if (perm.receive !== 'granted') {
    toast('Bạn chưa cho phép app gửi thông báo', 'error');
    return false;
  }
  await Push.removeAllListeners();
  Push.addListener('registration', (t) => saveToken(t.value).catch((e) => console.warn(e)));
  Push.addListener('registrationError', (e) => toast('Không đăng ký được thông báo: ' + (e.error || ''), 'error'));
  // Thông báo đến khi đang mở app: hiện toast
  Push.addListener('pushNotificationReceived', (n) => toast(`${n.title || ''}: ${n.body || ''}`));
  // Người dùng bấm vào thông báo trên thanh trạng thái
  Push.addListener('pushNotificationActionPerformed', (a) => {
    const id = a.notification?.data?.orderId;
    if (id) onOpenOrder(id);
  });
  await Push.register();
  return true;
}

/* ---------- PWA ---------- */
async function enableWeb() {
  const permission = await Notification.requestPermission();
  if (permission !== 'granted') {
    toast('Bạn chưa cho phép trình duyệt gửi thông báo', 'error');
    return false;
  }
  const registration = await navigator.serviceWorker.ready;
  const token = await getWebPushToken(registration, VAPID_KEY);
  await saveToken(token);
  onForegroundMessage((payload) => {
    const n = payload.notification || {};
    toast(`${n.title || 'Thông báo'}: ${n.body || ''}`);
  });
  return true;
}
