window.VC = (function () {
  const state = {
    card: '', title: '', link: '', date: '', summary: '', body: ''
  };

  function clean(t) {
    return (t || '').replace(/\u00a0/g, ' ').replace(/[ \t]+/g, ' ').replace(/\n{3,}/g, '\n\n').trim();
  }

  function visibleText(el) {
    if (!el) return '';
    const style = getComputedStyle(el);
    if (style.display === 'none' || style.visibility === 'hidden') return '';
    return clean(el.innerText || el.textContent || '');
  }

  function css(el) {
    if (!el || el === document.body) return 'body';
    if (el.id) return '#' + CSS.escape(el.id);
    const parts = [];
    let cur = el;
    while (cur && cur.nodeType === 1 && cur !== document.body) {
      let part = cur.tagName.toLowerCase();
      const cls = Array.from(cur.classList || []).filter(Boolean).filter(c => !c.startsWith('__vc_'));
      if (cls.length) part += '.' + cls.slice(0, 2).map(CSS.escape).join('.');
      const parent = cur.parentElement;
      if (parent) {
        const same = Array.from(parent.children).filter(x => x.tagName === cur.tagName);
        if (same.length > 1) part += ':nth-of-type(' + (same.indexOf(cur) + 1) + ')';
      }
      parts.unshift(part);
      cur = parent;
      if (parts.length > 6) break;
    }
    return parts.join(' > ');
  }

  function relCss(root, el) {
    if (!root || !el) return css(el);
    const all = Array.from(root.querySelectorAll('*'));
    for (const cand of all) {
      const s = css(cand).split(' > ').slice(-3).join(' > ');
      try { if (root.querySelector(s) === el) return s; } catch(e) {}
    }
    return css(el);
  }

  function mark(el, kind) {
    document.querySelectorAll('.__vc_pick__').forEach(x => x.classList.remove('__vc_pick__'));
    if (!document.getElementById('__vc_style__')) {
      const st = document.createElement('style');
      st.id = '__vc_style__';
      st.textContent = '.__vc_pick__{outline:4px solid #ffc400!important;outline-offset:2px!important;background:rgba(255,196,0,.16)!important}';
      document.documentElement.appendChild(st);
    }
    if (el) el.classList.add('__vc_pick__');
  }

  function pick(mode) {
    window.VisualCrawler.onStatus(mode + ' 선택 모드: 원하는 영역을 터치하세요.');
    const handler = function (ev) {
      ev.preventDefault(); ev.stopPropagation();
      const t = ev.changedTouches ? ev.changedTouches[0] : ev;
      const el = document.elementFromPoint(t.clientX, t.clientY);
      mark(el, mode);
      if (mode === 'card') {
        state.card = css(el);
        window.VisualCrawler.onPicked(JSON.stringify({mode, selector: state.card, text: visibleText(el)}));
      } else if (mode === 'body') {
        state.body = css(el);
        window.VisualCrawler.onPicked(JSON.stringify({mode, selector: state.body, text: visibleText(el)}));
      } else {
        const card = document.querySelector(state.card) || el.closest('article, li, .post, .item, .card') || el.parentElement;
        const rel = relCss(card, el);
        state[mode] = rel;
        window.VisualCrawler.onPicked(JSON.stringify({mode, selector: rel, text: visibleText(el)}));
      }
      document.removeEventListener('click', handler, true);
      document.removeEventListener('touchend', handler, true);
      return false;
    };
    document.addEventListener('click', handler, true);
    document.addEventListener('touchend', handler, true);
  }

  function setRules(json) {
    try { Object.assign(state, JSON.parse(json || '{}')); } catch(e) {}
  }

  function rules() { return JSON.stringify(state); }

  function field(card, key) {
    try {
      const sel = state[key];
      const el = sel ? card.querySelector(sel) : null;
      return visibleText(el);
    } catch(e) { return ''; }
  }

  function linkUrl(card) {
    try {
      const el = state.link ? card.querySelector(state.link) : card.querySelector('a[href]');
      return el && el.href ? el.href : '';
    } catch(e) { return ''; }
  }

  function extractList() {
    let cards = [];
    try { cards = Array.from(document.querySelectorAll(state.card)); } catch(e) {}
    if (!cards.length) {
      const one = document.querySelector('article, li, .post, .item, .card, body');
      if (one) cards = [one];
    }
    const items = cards.map((card, i) => ({
      index: i + 1,
      title: field(card, 'title'),
      linkText: field(card, 'link'),
      date: field(card, 'date'),
      summary: field(card, 'summary'),
      detailUrl: linkUrl(card)
    })).filter(x => x.title || x.linkText || x.summary || x.detailUrl);
    window.VisualCrawler.onExtracted(JSON.stringify({items, bodySelector: state.body, pageUrl: location.href, pageTitle: document.title || ''}));
  }

  function extractBody(selector) {
    let el = null;
    try { el = document.querySelector(selector || state.body); } catch(e) {}
    if (!el) el = document.querySelector('article, main, .content, .post, body');
    window.VisualCrawler.onBodyExtracted(visibleText(el));
  }

  return { pick, extractList, extractBody, setRules, rules };
})();
