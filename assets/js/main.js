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
})();
