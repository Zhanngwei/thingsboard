(function () {
  "use strict";

  function slugify(text, index) {
    var slug = text.trim().toLowerCase()
      .replace(/[`.()（）]/g, "")
      .replace(/[^a-z0-9\u4e00-\u9fa5]+/g, "-")
      .replace(/^-|-$/g, "");
    return slug || "section-" + index;
  }

  function renderMarkdown() {
    var source = document.getElementById("markdown-source");
    var target = document.getElementById("article");
    if (!source || !target || typeof window.markdownit !== "function") {
      return false;
    }
    var md = window.markdownit({ html: true, linkify: true, typographer: false });
    target.innerHTML = md.render(source.textContent);

    var used = Object.create(null);
    target.querySelectorAll("h2, h3").forEach(function (heading, index) {
      var base = slugify(heading.textContent, index);
      used[base] = (used[base] || 0) + 1;
      heading.id = used[base] === 1 ? base : base + "-" + used[base];
    });

    target.querySelectorAll("pre code.language-mermaid").forEach(function (code) {
      var container = document.createElement("div");
      container.className = "mermaid";
      container.textContent = code.textContent;
      code.parentElement.replaceWith(container);
    });
    return true;
  }

  function renderMermaid() {
    if (typeof window.mermaid === "undefined") {
      return;
    }
    window.mermaid.initialize({
      startOnLoad: false,
      securityLevel: "loose",
      theme: "neutral",
      flowchart: { htmlLabels: true, curve: "basis" },
      sequence: { useMaxWidth: true, wrap: true }
    });
    window.mermaid.run({ querySelector: ".mermaid" });
  }

  window.addEventListener("DOMContentLoaded", function () {
    if (renderMarkdown()) {
      renderMermaid();
    }
  });
}());

