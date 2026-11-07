// ==UserScript==
// @name         MindHeed
// @namespace    https://mindheed.app
// @version      0.2.0
// @description  Veils the Instagram home feed after a few posts and offers a bubble to your saved posts.
// @match        https://www.instagram.com/*
// @run-at       document-idle
// @inject-into  content
// @noframes
// ==/UserScript==

// iPhone test version (Safari + the free "Userscripts" extension). Same logic as the Chrome
// extension, adapted for mobile: only the main feed ("/") is veiled, saved posts never are.
// Add "@updateURL" / "@downloadURL" lines pointing to the raw GitHub file to get auto-updates.

(() => {
  "use strict";

  const SETTINGS = {
    clearPosts: 1, // 1 clear post...
    rampPosts: 2, // ...fully veiled by the 3rd
    maxBlur: 24,
  };
  const FLOATY_ID = "mindheed-floaty";
  const STYLE_ID = "mindheed-style";

  // Instagram removes posts that left the screen: remember each post's depth by its link,
  // so the 3-post count does not restart when the list recycles.
  const depthByKey = new Map();
  let nextDepth = 1;
  let floaty = null;
  let animationFrame = null;
  let animationStart = 0;
  let scheduled = false;

  injectStyle();
  ensureFloaty();
  new MutationObserver(schedule).observe(document.documentElement, { childList: true, subtree: true });
  window.addEventListener("scroll", schedule, { passive: true });
  setInterval(schedule, 1500);
  schedule();

  function schedule() {
    if (scheduled) return;
    scheduled = true;
    requestAnimationFrame(() => {
      scheduled = false;
      refresh();
    });
  }

  function isHomeFeed() {
    return location.pathname === "/" || location.pathname === "";
  }

  function feedPosts() {
    return Array.from(document.querySelectorAll("main article, article")).filter(
      (a) => a instanceof HTMLElement && a.querySelector("img, video"),
    );
  }

  function postKey(article) {
    const link = article.querySelector('a[href*="/p/"], a[href*="/reel/"]');
    return link ? new URL(link.href, location.origin).pathname : null;
  }

  function depthOf(article) {
    if (article.dataset.mindheedDepth) return Number(article.dataset.mindheedDepth);
    const key = postKey(article);
    let depth = key ? depthByKey.get(key) : undefined;
    if (depth === undefined) {
      depth = nextDepth++;
      if (key) depthByKey.set(key, depth);
    }
    article.dataset.mindheedDepth = String(depth);
    return depth;
  }

  function refresh() {
    const posts = feedPosts();
    if (!isHomeFeed()) {
      posts.forEach((p) => setBlur(p, 0));
      updateFloaty(0);
      return;
    }

    let strongest = 0;
    for (const post of posts) {
      const depth = depthOf(post);
      const raw = depth > SETTINGS.clearPosts ? (depth - SETTINGS.clearPosts) / SETTINGS.rampPosts : 0;
      const eased = easeOutCubic(Math.min(1, Math.max(0, raw)));
      setBlur(post, eased);
      if (isOnScreen(post)) strongest = Math.max(strongest, eased);
    }
    updateFloaty(strongest);
  }

  function isOnScreen(el) {
    const r = el.getBoundingClientRect();
    return r.bottom > 0 && r.top < window.innerHeight;
  }

  function setBlur(post, progress) {
    post.dataset.mindheedPost = "true";
    post.style.setProperty("--mindheed-progress", progress.toFixed(3));
    post.style.setProperty("--mindheed-blur", `${(progress * SETTINGS.maxBlur).toFixed(1)}px`);
  }

  function savedUrl() {
    const own = Array.from(document.querySelectorAll('a[href$="/saved/"], a[href*="/saved/all-posts"]'))[0];
    if (own) return own.href;
    const username = currentUsername();
    return username ? `${location.origin}/${username}/saved/all-posts/` : `${location.origin}/accounts/edit/`;
  }

  function currentUsername() {
    const reserved = ["accounts", "direct", "explore", "reels", "stories", "your_activity", "p", "reel"];
    const links = Array.from(document.querySelectorAll("nav a[href], [role='navigation'] a[href], a[href]"));
    for (const link of links) {
      const match = new URL(link.href, location.origin).pathname.match(/^\/([^/]+)\/?$/);
      if (match && !reserved.includes(match[1].toLowerCase()) && link.querySelector("img")) return match[1];
    }
    return "";
  }

  function ensureFloaty() {
    if (floaty) return floaty;
    floaty = document.createElement("a");
    floaty.id = FLOATY_ID;
    floaty.setAttribute("aria-label", "Open your saved posts");
    floaty.innerHTML =
      '<svg viewBox="0 0 64 64" fill="none"><path d="M32 8C36.6 16.5 44.5 24.1 54 28C44.5 31.9 36.6 39.5 32 48C27.4 39.5 19.5 31.9 10 28C19.5 24.1 27.4 16.5 32 8Z" fill="white"/><circle cx="32" cy="28" r="6" fill="#FFD166"/></svg>';
    floaty.addEventListener("click", (event) => {
      event.preventDefault();
      depthByKey.clear();
      nextDepth = 1;
      location.assign(savedUrl());
    });
    document.documentElement.appendChild(floaty);
    return floaty;
  }

  function updateFloaty(progress) {
    const el = ensureFloaty();
    const show = isHomeFeed() && progress >= 0.55;
    el.dataset.visible = String(show);
    if (show && progress >= 0.8) startWander();
    else stopWander();
  }

  function startWander() {
    if (animationFrame) return;
    const step = (t) => {
      if (!animationStart) animationStart = t;
      const e = t - animationStart;
      const x = Math.sin(e / 1400) * 40 + Math.cos(e / 2600) * 24;
      const y = Math.sin(e / 1100) * 40 + Math.cos(e / 2300) * 60;
      floaty.style.transform = `translate3d(${x.toFixed(1)}px, ${y.toFixed(1)}px, 0) rotate(${(Math.sin(e / 1500) * 16).toFixed(1)}deg)`;
      animationFrame = requestAnimationFrame(step);
    };
    animationFrame = requestAnimationFrame(step);
  }

  function stopWander() {
    if (animationFrame) cancelAnimationFrame(animationFrame);
    animationFrame = null;
    animationStart = 0;
    if (floaty) floaty.style.transform = "";
  }

  function easeOutCubic(v) {
    return 1 - Math.pow(1 - v, 3);
  }

  function injectStyle() {
    if (document.getElementById(STYLE_ID)) return;
    const style = document.createElement("style");
    style.id = STYLE_ID;
    style.textContent = `
      [data-mindheed-post] { position: relative; }
      [data-mindheed-post] > * { filter: blur(var(--mindheed-blur, 0px)); transition: filter 240ms ease; }
      #${FLOATY_ID} {
        position: fixed; top: 22vh; right: 6vw; z-index: 2147483647;
        width: 60px; height: 60px; display: grid; place-items: center; border-radius: 999px;
        opacity: 0; pointer-events: none; transition: opacity 260ms ease;
        background: conic-gradient(from 210deg, #5B4BC4, #2B2D6E, #FFD166, #5B4BC4);
        box-shadow: 0 16px 40px rgba(43, 45, 110, .35);
        -webkit-tap-highlight-color: transparent;
      }
      #${FLOATY_ID}[data-visible="true"] { opacity: 1; pointer-events: auto; }
      #${FLOATY_ID} svg { width: 26px; height: 26px; }
    `;
    document.documentElement.appendChild(style);
  }
})();
