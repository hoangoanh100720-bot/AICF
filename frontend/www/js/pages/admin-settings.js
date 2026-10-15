/**
 * CÀI ĐẶT (ADMIN):
 *  - Thông tin quán: PUT /api/settings/shop
 *  - Trợ lý AI: chọn Gemini / Claude, model, khóa API (PUT /api/settings/ai). Máy chủ không bao giờ trả lại khóa,
 *    chỉ cho biết đã nhập hay chưa; để trống ô khóa = giữ khóa cũ.
 */
import { h } from '../core/dom.js';
import { appState } from '../core/store.js';
import { settingsRepo } from '../data/repositories.js';
import { asyncView, field, iconButton, readForm, setHeader, showFieldErrors, toast, toastError } from '../ui/components.js';
import { icons } from '../ui/icons.js';

const SHOP_FIELDS = [
  { name: 'name', label: 'Tên quán', required: true },
  { name: 'address', label: 'Địa chỉ' },
  { name: 'hours', label: 'Giờ mở cửa', placeholder: '7:00 - 22:00 hằng ngày' },
];

export function render() {
  setHeader('Cài đặt', iconButton(icons.back, 'Quay lại', () => history.back()));
  return asyncView(async () => ({ shop: await settingsRepo.shop(), ai: await settingsRepo.ai() }), ({ shop, ai }, reload) => {
    const aiFields = [
      { name: 'provider', label: 'Dịch vụ AI', type: 'select', required: true, options: [{ value: 'gemini', label: 'Google Gemini' }, { value: 'claude', label: 'Anthropic Claude' }] },
      { name: 'geminiModel', label: 'Model Gemini' },
      { name: 'geminiApiKey', label: 'Khóa API Gemini', type: 'password', autocomplete: 'off', hint: ai.geminiKeySet ? '✅ Đã có khóa - để trống để giữ nguyên' : 'Chưa có khóa (lấy tại aistudio.google.com)' },
      { name: 'claudeModel', label: 'Model Claude' },
      { name: 'claudeApiKey', label: 'Khóa API Claude', type: 'password', autocomplete: 'off', hint: ai.claudeKeySet ? '✅ Đã có khóa - để trống để giữ nguyên' : 'Chưa có khóa (lấy tại console.anthropic.com)' },
    ];
    return h('div', { class: 'stack' },
      section('Thông tin quán', SHOP_FIELDS, shop, async (data) => {
        const updated = await settingsRepo.updateShop(data);
        appState.set({ shop: updated });
        toast('Đã lưu thông tin quán', 'success');
      }),
      h('div', { class: 'banner ' + (shop.aiMode === 'LLM' ? 'banner-info' : 'banner-warn') },
        shop.aiMode === 'LLM'
          ? '✨ Trợ lý đang dùng AI thật.'
          : 'Trợ lý đang ở chế độ cơ bản (trả lời theo từ khóa). Nhập khóa API để dùng AI thật.'),
      section('Trợ lý AI', aiFields, ai, async (data) => {
        await settingsRepo.updateAi(data);
        toast('Đã lưu cấu hình AI', 'success');
        reload(true);
      }),
      h('div', { class: 'banner ' + (shop.firebaseEnabled ? 'banner-info' : 'banner-warn') },
        shop.firebaseEnabled
          ? '🔥 Firebase đã kết nối: đăng nhập Google và thông báo đẩy đang bật.'
          : '🔥 Máy chủ chưa cấu hình Firebase (xem README, mục Firebase). App vẫn chạy, thông báo chỉ có trong app.'));
  });
}

/** 1 thẻ form cài đặt (dùng lại field / readForm của components.js). */
function section(title, fields, initial, onSave) {
  const submit = h('button', { class: 'btn', type: 'submit' }, 'Lưu');
  const form = h('form', { class: 'card stack', novalidate: true },
    h('h2', { style: { fontSize: 'var(--text-lg)' } }, title),
    fields.map((f) => field(f, f.type === 'password' ? '' : initial[f.name])),
    submit);
  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    if (!form.reportValidity()) return;
    submit.disabled = true;
    try {
      await onSave(readForm(form, fields));
    } catch (err) {
      if (!showFieldErrors(form, err)) toastError(err);
    } finally {
      submit.disabled = false;
    }
  });
  return form;
}
