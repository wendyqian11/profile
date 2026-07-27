/* ---------------------------------------------------------------------------
   Browser-side interactivity:
     1. Dark / light theme toggle (remembers your choice in localStorage)
     2. Live search box that filters the blog list as you type
     3. New Post composer; per-post edit, pin-to-top, and delete
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

  /* ---- 3. New post: publish, edit, pin & delete ---- */

  var list = document.querySelector(".post-list");

  function pdtStamp() {
    try {
      return new Date().toLocaleString("en-US", {
        timeZone: "America/Los_Angeles",
        month: "short", day: "numeric", year: "numeric",
        hour: "numeric", minute: "2-digit", timeZoneName: "short"
      });
    } catch (e) { return new Date().toString(); }
  }

  // Ordering: pinned posts sit at the top (most recently pinned highest); all
  // other posts keep their natural order (newer above older). State lives on
  // each item: data-pinned/data-pinseq for pinning, data-natrank for order.
  var pinCounter = 0;
  var userRank = 0;

  function applyOrder() {
    if (!list) return;
    var arr = Array.prototype.slice.call(list.querySelectorAll(".post-item"));
    arr.sort(function (a, b) {
      var pa = a.getAttribute("data-pinned") === "1";
      var pb = b.getAttribute("data-pinned") === "1";
      if (pa !== pb) return pa ? -1 : 1;
      if (pa && pb) return (+b.getAttribute("data-pinseq")) - (+a.getAttribute("data-pinseq"));
      return (+a.getAttribute("data-natrank")) - (+b.getAttribute("data-natrank"));
    });
    arr.forEach(function (li) { list.appendChild(li); });
  }

  function updatePinButton(li) {
    var btn = li.querySelector(".post-pin");
    if (btn) btn.textContent = li.getAttribute("data-pinned") === "1" ? "Unpin" : "Pin";
  }

  function togglePin(li) {
    if (li.getAttribute("data-pinned") === "1") {
      li.setAttribute("data-pinned", "0");
      li.removeAttribute("data-pinseq");
    } else {
      li.setAttribute("data-pinned", "1");
      li.setAttribute("data-pinseq", String(++pinCounter));
    }
    updatePinButton(li);
    applyOrder();
  }

  // Set a post's text everywhere it appears: the title link, the excerpt, and
  // the data-search haystack that the filter reads.
  function setPostText(li, text) {
    var link = li.querySelector(".post-link");
    var body = li.querySelector(".post-excerpt");
    if (link) link.textContent = text;
    if (body) body.textContent = text;
    li.setAttribute("data-search", (text + " " + text).toLowerCase());
  }

  function enterEdit(li) {
    if (li.querySelector(".post-edit-input")) return; // already editing
    var link = li.querySelector(".post-link");
    var input = document.createElement("input");
    input.type = "text";
    input.className = "post-edit-input";
    input.value = link ? link.textContent : "";
    li.insertBefore(input, li.firstChild);
    var btn = li.querySelector(".post-edit");
    if (btn) btn.textContent = "Save";
    if (input.focus) input.focus();
  }

  function saveEdit(li) {
    var input = li.querySelector(".post-edit-input");
    if (!input) return;
    var text = input.value.trim();
    if (text) setPostText(li, text);
    li.removeChild(input);
    var btn = li.querySelector(".post-edit");
    if (btn) btn.textContent = "Edit";
  }

  // Build a post list-item for the entered text. The single input is used as
  // both the title and the body; the item carries its own edit, pin, and delete
  // buttons.
  function makePost(text) {
    var li = document.createElement("li");
    li.className = "post-item";
    li.setAttribute("data-search", (text + " " + text).toLowerCase());
    li.setAttribute("data-pinned", "0");
    li.setAttribute("data-natrank", String(--userRank));

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

    var edit = document.createElement("button");
    edit.type = "button";
    edit.className = "post-edit";
    edit.textContent = "Edit";

    var pin = document.createElement("button");
    pin.type = "button";
    pin.className = "post-pin";
    pin.textContent = "Pin";

    var del = document.createElement("button");
    del.type = "button";
    del.className = "post-delete";
    del.textContent = "Delete";

    li.appendChild(link);
    li.appendChild(time);
    li.appendChild(body);
    li.appendChild(edit);
    li.appendChild(pin);
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
    if (!input || !list) return;
    var text = input.value.trim();
    if (!text) return; // ignore empty submissions
    list.insertBefore(makePost(text), list.firstChild);
    applyOrder();
    input.value = "";
    closeModal();
  }

  // Event delegation so this works regardless of when the markup is added.
  document.addEventListener("click", function (e) {
    var t = e.target;
    if (!t) return;
    if (t.id === "new-post-trigger") { openModal(); return; }
    if (t.id === "new-post-submit") { publish(); return; }
    if (t.classList && t.classList.contains("post-edit")) {
      var editItem = t.closest(".post-item");
      if (editItem) {
        if (editItem.querySelector(".post-edit-input")) saveEdit(editItem);
        else enterEdit(editItem);
      }
      return;
    }
    if (t.classList && t.classList.contains("post-pin")) {
      var pinItem = t.closest(".post-item");
      if (pinItem) togglePin(pinItem);
      return;
    }
    if (t.classList && t.classList.contains("post-delete")) {
      var item = t.closest(".post-item");
      if (item && item.parentNode) item.parentNode.removeChild(item);
    }
  });

  // Give the generated posts their natural rank (document order) and a default
  // unpinned state, then arrange the list.
  if (list) {
    Array.prototype.forEach.call(list.querySelectorAll(".post-item"), function (li, i) {
      li.setAttribute("data-natrank", String(i));
      if (li.getAttribute("data-pinned") === null) li.setAttribute("data-pinned", "0");
    });
    applyOrder();
  }
})();
