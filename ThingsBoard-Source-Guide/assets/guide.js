(function () {
  "use strict";

  var SVG_NS = "http://www.w3.org/2000/svg";
  var HIGH_CONTRAST_MERMAID_THEME = {
    background: "#ffffff",
    primaryColor: "#ffffff",
    primaryTextColor: "#111827",
    primaryBorderColor: "#334155",
    secondaryColor: "#eef6f8",
    secondaryTextColor: "#111827",
    secondaryBorderColor: "#176b87",
    tertiaryColor: "#fff7e8",
    tertiaryTextColor: "#111827",
    tertiaryBorderColor: "#9a5b13",
    textColor: "#111827",
    lineColor: "#253746",
    mainBkg: "#ffffff",
    nodeBorder: "#334155",
    clusterBkg: "#f8fafc",
    clusterBorder: "#64748b",
    edgeLabelBackground: "#ffffff",
    actorBkg: "#ffffff",
    actorBorder: "#334155",
    actorTextColor: "#111827",
    actorLineColor: "#64748b",
    signalColor: "#253746",
    signalTextColor: "#111827",
    labelBoxBkgColor: "#f8fafc",
    labelBoxBorderColor: "#64748b",
    labelTextColor: "#111827",
    loopTextColor: "#111827",
    noteBkgColor: "#fff5cc",
    noteBorderColor: "#8a5a00",
    noteTextColor: "#111827",
    activationBkgColor: "#e7f0f4",
    activationBorderColor: "#176b87",
    sequenceNumberColor: "#ffffff"
  };

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
      var container = document.createElement("pre");
      container.className = "mermaid";
      container.textContent = code.textContent;
      code.parentElement.replaceWith(container);
    });
    return true;
  }

  function mermaidConfig() {
    return {
      startOnLoad: false,
      securityLevel: "loose",
      theme: "base",
      themeVariables: HIGH_CONTRAST_MERMAID_THEME,
      flowchart: { htmlLabels: true, curve: "basis", useMaxWidth: true },
      sequence: { useMaxWidth: true, wrap: true }
    };
  }

  function svgDimensions(svg) {
    var viewBox = (svg.getAttribute("viewBox") || "").trim().split(/[\s,]+/).map(Number);
    if (viewBox.length === 4 && viewBox.every(Number.isFinite)) {
      return { width: Math.ceil(viewBox[2]), height: Math.ceil(viewBox[3]) };
    }
    return {
      width: Math.ceil(parseFloat(svg.getAttribute("width")) || svg.getBoundingClientRect().width),
      height: Math.ceil(parseFloat(svg.getAttribute("height")) || svg.getBoundingClientRect().height)
    };
  }

  function standaloneSvgSource(svg, title) {
    var clone = svg.cloneNode(true);
    var size = svgDimensions(svg);
    clone.setAttribute("xmlns", SVG_NS);
    clone.setAttribute("width", String(size.width));
    clone.setAttribute("height", String(size.height));
    clone.setAttribute("data-standalone-svg", "true");
    clone.style.width = size.width + "px";
    clone.style.height = size.height + "px";
    clone.style.maxWidth = "none";
    clone.style.background = "#ffffff";

    var background = document.createElementNS(SVG_NS, "rect");
    background.setAttribute("x", "0");
    background.setAttribute("y", "0");
    background.setAttribute("width", "100%");
    background.setAttribute("height", "100%");
    background.setAttribute("fill", "#ffffff");
    background.setAttribute("data-diagram-background", "true");
    clone.insertBefore(background, clone.firstChild);

    if (!clone.querySelector("title")) {
      var titleNode = document.createElementNS(SVG_NS, "title");
      titleNode.textContent = title;
      clone.insertBefore(titleNode, background.nextSibling);
    }
    return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + new XMLSerializer().serializeToString(clone);
  }

  function openStandaloneSvg(svg, title) {
    var blob = new Blob([standaloneSvgSource(svg, title)], { type: "image/svg+xml;charset=utf-8" });
    var url = URL.createObjectURL(blob);
    window.open(url, "_blank", "noopener,noreferrer");
    window.setTimeout(function () { URL.revokeObjectURL(url); }, 60000);
  }

  function decorateStaticSvgImages() {
    document.querySelectorAll(".content img").forEach(function (img) {
      var src = (img.getAttribute("src") || "").split(/[?#]/)[0].toLowerCase();
      if (!src.endsWith(".svg")) {
        return;
      }
      var link = img.closest("a");
      if (!link) {
        link = document.createElement("a");
        img.parentNode.insertBefore(link, img);
        link.appendChild(img);
      }
      if (!link.getAttribute("href")) {
        link.setAttribute("href", img.getAttribute("src"));
      }
      link.setAttribute("target", "_blank");
      link.setAttribute("rel", "noopener noreferrer");
      link.setAttribute("title", "在新窗口打开原始 SVG");
      link.setAttribute("aria-label", (img.getAttribute("alt") || "图表") + "，在新窗口打开原始 SVG");
      link.classList.add("diagram-thumbnail", "static-svg-thumbnail");
      img.classList.add("diagram-thumbnail-image");
    });
  }

  function decorateMermaidDiagrams() {
    document.querySelectorAll(".mermaid").forEach(function (container, index) {
      if (!container.querySelector("svg")) {
        return;
      }
      container.classList.add("diagram-thumbnail", "mermaid-thumbnail");
      container.setAttribute("role", "link");
      container.setAttribute("tabindex", "0");
      container.setAttribute("title", "在新窗口打开原始 SVG");
      container.setAttribute("aria-label", "流程图 " + (index + 1) + "，在新窗口打开原始 SVG");
      if (container.dataset.svgViewerBound === "true") {
        return;
      }
      container.dataset.svgViewerBound = "true";
      container.addEventListener("click", function () {
        openStandaloneSvg(container.querySelector("svg"), document.title + " - diagram " + (index + 1));
      });
      container.addEventListener("keydown", function (event) {
        if (event.key === "Enter" || event.key === " ") {
          event.preventDefault();
          openStandaloneSvg(container.querySelector("svg"), document.title + " - diagram " + (index + 1));
        }
      });
    });
  }

  function renderMermaid() {
    if (typeof window.mermaid === "undefined") {
      return Promise.resolve();
    }
    window.mermaid.initialize(mermaidConfig());
    return window.mermaid.run({ querySelector: ".mermaid" }).then(decorateMermaidDiagrams);
  }

  function waitForImages() {
    return Promise.all(Array.from(document.images).map(function (img) {
      if (img.complete) {
        return Promise.resolve();
      }
      return new Promise(function (resolve) {
        img.addEventListener("load", resolve, { once: true });
        img.addEventListener("error", resolve, { once: true });
      });
    }));
  }

  function scrollToHash() {
    var target = window.location.hash ? document.getElementById(window.location.hash.slice(1)) : null;
    if (target) {
      target.scrollIntoView();
    }
  }

  window.tbGuide = {
    mermaidConfig: mermaidConfig,
    decorateMermaidDiagrams: decorateMermaidDiagrams,
    decorateStaticSvgImages: decorateStaticSvgImages,
    standaloneSvgSource: standaloneSvgSource
  };

  window.addEventListener("DOMContentLoaded", function () {
    renderMarkdown();
    decorateStaticSvgImages();
    renderMermaid()
      .then(waitForImages)
      .then(scrollToHash)
      .catch(function (error) { console.error("Failed to render diagrams", error); });
    window.addEventListener("hashchange", function () { window.requestAnimationFrame(scrollToHash); });
  });
}());
