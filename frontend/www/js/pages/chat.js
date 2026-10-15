/**
 * MÀN HÌNH CHAT VỚI TRỢ LÝ AI (dùng lại Trợ lý AI của dự án AICF qua REST API).
 *   GET /api/chat/status  -> đang dùng AI thật hay chế độ cơ bản + câu hỏi gợi ý
 *   GET /api/chat         -> lịch sử
 *   POST /api/chat        -> gửi câu hỏi
 *   DELETE /api/chat      -> xóa lịch sử
 * Khách hỏi thực đơn / gợi ý món / đơn của mình; nhân viên hỏi thêm doanh thu, món bán chạy, đơn đang chờ.
 */
import { h, mount } from '../core/dom.js';
import { dateTime } from '../core/format.js';
import { chatRepo } from '../data/repositories.js';
import { confirmDialog, errorState, iconButton, setHeader, skeleton, toastError } from '../ui/components.js';
import { icons } from '../ui/icons.js';

export function render() {
  const list = h('div', { class: 'chat', role: 'log', 'aria-live': 'polite', 'aria-label': 'Cuộc trò chuyện' });
  const suggestionsBox = h('div', { class: 'chips', 'aria-label': 'Câu hỏi gợi ý' });
  const modeTag = h('span', { class: 'tag' }, '…');
  const input = h('input', { class: 'input', type: 'text', placeholder: 'Hỏi Trợ lý AI…', 'aria-label': 'Câu hỏi', maxlength: 2000, autocomplete: 'off' });
  const sendBtn = h('button', { class: 'btn', type: 'submit', 'aria-label': 'Gửi', html: icons.send, style: { width: '48px', padding: 0 } });

  setHeader('Trợ lý AI', iconButton(icons.trash, 'Xóa lịch sử chat', async () => {
    if (!(await confirmDialog('Xóa toàn bộ lịch sử trò chuyện?', { okLabel: 'Xóa', danger: true }))) return;
    try {
      await chatRepo.clear();
      mount(list, welcome());
    } catch (err) { toastError(err); }
  }));

  const bubble = (role, content, meta) => h('div', { class: 'msg ' + (role === 'USER' ? 'msg-user' : 'msg-ai') },
    content, meta ? h('div', { class: 'msg-meta' }, meta) : null);
  const scrollDown = () => requestAnimationFrame(() => window.scrollTo({ top: document.body.scrollHeight }));

  const showSuggestions = (items) => mount(suggestionsBox, (items || []).map((s) =>
    h('button', { class: 'chip', type: 'button', onclick: () => send(s) }, s)));

  async function send(text) {
    const message = (text ?? input.value).trim();
    if (!message || sendBtn.disabled) return;
    input.value = '';
    list.querySelector('.empty')?.remove();
    list.appendChild(bubble('USER', message));
    const typing = h('div', { class: 'msg msg-ai', 'aria-label': 'AI đang trả lời' },
      h('span', { class: 'msg-typing' }, h('i'), h('i'), h('i')));
    list.appendChild(typing);
    scrollDown();
    sendBtn.disabled = true;
    try {
      const r = await chatRepo.ask(message);
      typing.replaceWith(bubble('ASSISTANT', r.reply, r.mode === 'LLM' ? 'AI' : 'Chế độ cơ bản'));
      if (r.notice) list.appendChild(h('div', { class: 'banner banner-warn' }, r.notice));
      showSuggestions(r.suggestions);
    } catch (err) {
      typing.remove();
      toastError(err);
      input.value = message; // giữ lại câu hỏi để gửi lại
    } finally {
      sendBtn.disabled = false;
      scrollDown();
      input.focus();
    }
  }

  const form = h('form', { onsubmit: (e) => { e.preventDefault(); send(); } }, input, sendBtn);

  // Tải trạng thái + lịch sử
  mount(list, skeleton(3, 56));
  Promise.all([chatRepo.status(), chatRepo.history(50)])
    .then(([status, history]) => {
      modeTag.textContent = status.mode === 'LLM' ? '✨ AI · ' + status.provider : 'Chế độ cơ bản';
      showSuggestions(status.suggestions);
      mount(list, history.length
        ? history.map((m) => bubble(m.role, m.content, dateTime(m.createdAt)))
        : welcome());
      scrollDown();
    })
    .catch((err) => mount(list, errorState(err)));

  return h('div', { class: 'chat-page stack' },
    h('div', { class: 'row-between' }, h('span', { class: 'muted small' }, 'Trợ lý trả lời dựa trên dữ liệu thật của quán'), modeTag),
    list,
    h('div', { class: 'composer' }, h('div', { style: { maxWidth: 'var(--content-max)', margin: '0 auto var(--space-2)' } }, suggestionsBox), form));
}

function welcome() {
  return h('div', { class: 'empty' },
    h('div', { class: 'emoji', 'aria-hidden': 'true' }, '🤖'),
    h('strong', null, 'Xin chào! Mình là Trợ lý AI của quán'),
    h('p', null, 'Hỏi mình về thực đơn, gợi ý món, hay tình trạng đơn của bạn nhé.'));
}
