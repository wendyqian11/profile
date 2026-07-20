/* ---------------------------------------------------------------------------
   Browser-side interactivity:
     1. Dark / light theme toggle (remembers your choice in localStorage)
     2. Live search box that filters the blog list as you type
   --------------------------------------------------------------------------- */
(function () {
  "use strict";

  /* ---- 1. Theme toggle ---- */
  var root = document.documentElement;
  var STORAGE_KEY = "theme";

  function applyTheme(theme) {
    root.setAttribute("data-theme", theme);
    var btn = document.getElementById("theme-toggle");
    if (btn) btn.textContent = theme === "dark" ? "☀️" : "🌙";
  }

  // Use a saved preference, otherwise follow the OS setting.
  var saved = null;
  try { saved = localStorage.getItem(STORAGE_KEY); } catch (e) { /* ignore */ }
  var prefersDark = window.matchMedia &&
    window.matchMedia("(prefers-color-scheme: dark)").matches;
  applyTheme(saved || (prefersDark ? "dark" : "light"));

  document.addEventListener("click", function (e) {
    if (!e.target || e.target.id !== "theme-toggle") return;
    var next = root.getAttribute("data-theme") === "dark" ? "light" : "dark";
    applyTheme(next);
    try { localStorage.setItem(STORAGE_KEY, next); } catch (err) { /* ignore */ }
  });

  /* ---- 2. Blog search filter ---- */
  var search = document.getElementById("post-search");
  if (search) {
    var items = Array.prototype.slice.call(document.querySelectorAll(".post-item"));
    var empty = document.querySelector(".no-results");

    search.addEventListener("input", function () {
      var q = search.value.trim().toLowerCase();
      var visible = 0;
      items.forEach(function (li) {
        var haystack = li.getAttribute("data-search") || "";
        var match = haystack.indexOf(q) !== -1;
        li.hidden = !match;
        if (match) visible++;
      });
      if (empty) empty.hidden = visible !== 0;
    });
  }

  /* ---- 3. New post: publish & delete ---- */

  // Current date & time in Pacific time, e.g. "Jul 13, 2026, 2:30 PM PDT".
  function pdtStamp() {
    try {
      return new Date().toLocaleString("en-US", {
        timeZone: "America/Los_Angeles",
        month: "short", day: "numeric", year: "numeric",
        hour: "numeric", minute: "2-digit",
        timeZoneName: "short"
      });
    } catch (e) {
      return new Date().toString();
    }
  }

  // Build a post list-item for the entered text. The single input is used as
  // both the title and the body, and the item carries its own delete button.
  function makePost(text) {
    var li = document.createElement("li");
    li.className = "post-item";
    li.setAttribute("data-search", (text + " " + text).toLowerCase());

    var link = document.createElement("a");
    link.className = "post-link";
    link.setAttribute("href", "#");
    link.textContent = text;

    var time = document.createElement("time");
    time.className = "post-date";
    time.textContent = pdtStamp();

    var body = document.createElement("p");
    body.className = "post-excerpt";
    body.textContent = text;

    var del = document.createElement("button");
    del.type = "button";
    del.className = "post-delete";
    del.textContent = "Delete";

    li.appendChild(link);
    li.appendChild(time);
    li.appendChild(body);
    li.appendChild(del);
    return li;
  }

  function openModal() {
    var modal = document.getElementById("new-post-modal");
    if (modal) modal.hidden = false;
    var input = document.getElementById("new-post-input");
    if (input) input.focus();
  }

  function closeModal() {
    var modal = document.getElementById("new-post-modal");
    if (modal) modal.hidden = true;
  }

  function publish() {
    var input = document.getElementById("new-post-input");
    var list = document.querySelector(".post-list");
    if (!input || !list) return;
    var text = input.value.trim();
    if (!text) return; // ignore empty submissions
    list.insertBefore(makePost(text), list.firstChild);
    input.value = "";
    closeModal();
  }

  // Event delegation so this works regardless of when the markup is added.
  document.addEventListener("click", function (e) {
    var t = e.target;
    if (!t) return;
    if (t.id === "new-post-trigger") { openModal(); return; }
    if (t.id === "new-post-submit") { publish(); return; }
    if (t.classList && t.classList.contains("post-delete")) {
      var item = t.closest(".post-item");
      if (item && item.parentNode) item.parentNode.removeChild(item);
    }
  });
})();
