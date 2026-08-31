const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..');
const readmePath = path.join(root, 'README.md');
const outputPath = path.join(root, 'index.html');
const markdown = fs.readFileSync(readmePath, 'utf8').replace(/^\uFEFF/, '');

if (markdown.includes('</script>')) {
  throw new Error(`Cannot embed a literal </script> from ${readmePath}`);
}

const titleMatch = markdown.match(/^#\s+(.+)$/m);
if (!titleMatch) {
  throw new Error(`Missing H1 in ${readmePath}`);
}

const title = titleMatch[1].trim();
const sections = [
  ['阅读定位', '阅读定位'],
  ['当前进度', '当前进度'],
  ['01-14 接入与实体', '第-01-章关键结论-mqtt-消息进入系统'],
  ['15-22 Actor 与存储', '第-15-章关键结论-actor-模型'],
  ['23-29 协议与会话', '第-23-章关键结论-http-设备-api-流程'],
  ['30-36 查询与实时', '第-30-章关键结论-telemetry-查询流程'],
  ['37-43 平台与运行时', '第-37-章关键结论-rule-node-外部集成流程'],
  ['图表约定', '图表约定'],
  ['版本边界', '版本边界']
];
const sidebar = sections.map(([label, anchor]) => `<a href="#${anchor}">${label}</a>`).join('');

const html = `<!doctype html>
<html lang="zh-CN">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <link rel="icon" href="data:,">
  <title>${title} | ThingsBoard Source Guide</title>
  <link rel="stylesheet" href="assets/styles.css">
  <script src="https://cdn.jsdelivr.net/npm/markdown-it@14/dist/markdown-it.min.js"></script>
  <script src="https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.min.js"></script>
</head>
<body>
  <header class="topbar">
    <a class="brand" href="index.html">ThingsBoard Source Guide</a>
    <nav class="topnav" aria-label="文档导航"><a href="README.md">Markdown</a><a href="SUMMARY.md">全书目录</a></nav>
  </header>
  <main class="layout">
    <aside class="sidebar" aria-label="全书目录">
      <strong>${title}</strong>
      ${sidebar}
    </aside>
    <article id="article" class="content" aria-live="polite"></article>
  </main>
  <footer class="footer">Source baseline: ThingsBoard release-3.6 · 69124284c2</footer>
  <script id="markdown-source" type="text/plain">${markdown}
</script>
  <script src="assets/guide.js"></script>
</body>
</html>
`;

fs.writeFileSync(outputPath, html, 'utf8');
console.log(`Generated ${path.relative(root, outputPath)}`);
