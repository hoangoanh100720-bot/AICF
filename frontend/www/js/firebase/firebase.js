/**
 * FIREBASE JS SDK (phía app) - tải ĐỘNG từ CDN của Google chỉ khi cần, nên app khởi động không bị chậm
 * và vẫn chạy khi chưa cấu hình Firebase.
 *
 * Luồng đăng nhập Google:
 *   app -> Firebase Auth (cửa sổ Google) -> nhận ID token -> POST /api/auth/firebase
 *   -> backend kiểm tra token bằng Firebase Admin SDK -> trả JWT của hệ thống.
 */
import { IS_NATIVE } from '../config.js';
import { FIREBASE_SDK_VERSION, firebaseConfig } from './firebase-config.js';

const CDN = `https://www.gstatic.com/firebasejs/${FIREBASE_SDK_VERSION}`;
let appPromise = null;

/** Đã điền cấu hình Firebase chưa. */
export const isFirebaseConfigured = () => !!firebaseConfig.apiKey && !!firebaseConfig.projectId;

/** Đăng nhập Google chạy được ở PWA/trình duyệt. (Trong app Android, cửa sổ Google bị WebView chặn.) */
export const canUseGoogleSignIn = () => isFirebaseConfigured() && !IS_NATIVE;

/** Khởi tạo FirebaseApp 1 lần. */
async function firebaseApp() {
  if (!isFirebaseConfigured()) throw new Error('Chưa cấu hình Firebase (js/firebase/firebase-config.js)');
  if (!appPromise) {
    appPromise = import(`${CDN}/firebase-app.js`).then(({ initializeApp }) => initializeApp(firebaseConfig));
  }
  return appPromise;
}

/**
 * Mở cửa sổ đăng nhập Google, trả về Firebase ID token để gửi cho backend.
 * @returns {Promise<string>}
 */
export async function googleSignIn() {
  const app = await firebaseApp();
  const { getAuth, GoogleAuthProvider, signInWithPopup } = await import(`${CDN}/firebase-auth.js`);
  const auth = getAuth(app);
  const result = await signInWithPopup(auth, new GoogleAuthProvider());
  return result.user.getIdToken();
}

/** Đăng xuất khỏi Firebase (nếu đã đăng nhập Google). */
export async function firebaseSignOut() {
  if (!appPromise) return;
  const { getAuth, signOut } = await import(`${CDN}/firebase-auth.js`);
  await signOut(getAuth(await appPromise)).catch(() => {});
}

/**
 * Lấy mã FCM của trình duyệt (Web Push) dùng service worker của app.
 * @param {ServiceWorkerRegistration} registration
 * @param {string} vapidKey
 */
export async function getWebPushToken(registration, vapidKey) {
  const app = await firebaseApp();
  const { getMessaging, getToken, isSupported } = await import(`${CDN}/firebase-messaging.js`);
  if (!(await isSupported())) throw new Error('Trình duyệt này không hỗ trợ thông báo đẩy');
  return getToken(getMessaging(app), { vapidKey, serviceWorkerRegistration: registration });
}

/** Nghe thông báo đến khi app ĐANG MỞ (lúc đó trình duyệt không tự hiện thông báo hệ thống). */
export async function onForegroundMessage(handler) {
  const app = await firebaseApp();
  const { getMessaging, onMessage } = await import(`${CDN}/firebase-messaging.js`);
  onMessage(getMessaging(app), handler);
}
