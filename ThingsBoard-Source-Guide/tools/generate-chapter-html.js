const fs = require('fs');
const path = require('path');

const [chapterDirArg] = process.argv.slice(2);
if (!chapterDirArg) {
  throw new Error('Usage: node tools/generate-chapter-html.js chapters/<chapter>');
}

const root = path.resolve(__dirname, '..');
const chapterDir = path.resolve(root, chapterDirArg);
const readmePath = path.join(chapterDir, 'README.md');
const outputPath = path.join(chapterDir, 'index.html');
const markdown = fs.readFileSync(readmePath, 'utf8').replace(/^\uFEFF/, '');

if (markdown.includes('</script>')) {
  throw new Error(`Cannot embed a literal </script> from ${readmePath}`);
}

const titleMatch = markdown.match(/^#\s+(.+)$/m);
if (!titleMatch) {
  throw new Error(`Missing H1 in ${readmePath}`);
}

const title = titleMatch[1].trim();
const htmlMarkdown = markdown.replace(/(\.\.\/\d{2}-[^/)]+)\/README\.md/g, '$1/index.html');
const sections = [
  ['一、流程目标', '一-流程目标'],
  ['二、入口', '二-入口'],
  ['三、完整调用链', '三-完整调用链'],
  ['四、消息流', '四-消息流'],
  ['五、时序图', '五-时序图'],
  ['六、数据变化', '六-数据变化'],
  ['七、源码分析', '七-源码分析'],
  ['八、Actor 分析', '八-actor-分析'],
  ['九、Kafka 分析', '九-kafka-分析'],
  ['十、数据库分析', '十-数据库分析'],
  ['十一、异常处理', '十一-异常处理'],
  ['十二、源码阅读路线', '十二-源码阅读路线'],
  ['十三、常见面试题', '十三-常见面试题']
];
const sidebar = sections.map(([label, anchor]) => `<a href="#${anchor}">${label}</a>`).join('');

const html = `<!doctype html>
<html lang="zh-CN">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>${title} | ThingsBoard Source Guide</title>
  <link rel="stylesheet" href="../../assets/styles.css">
  <script src="https://cdn.jsdelivr.net/npm/markdown-it@14/dist/markdown-it.min.js"></script>
  <script src="https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.min.js"></script>
</head>
<body>
  <header class="topbar">
    <a class="brand" href="../../index.html">ThingsBoard Source Guide</a>
    <nav class="topnav" aria-label="文档导航"><a href="README.md">Markdown</a><a href="../../index.html">全书目录</a><a href="sequence.svg" target="_blank" rel="noopener noreferrer">时序图 SVG</a></nav>
  </header>
  <main class="layout">
    <aside class="sidebar" aria-label="章节目录">
      <strong>${title}</strong>
      ${sidebar}
    </aside>
    <article id="article" class="content" aria-live="polite"></article>
  </main>
  <footer class="footer">Source baseline: ThingsBoard 3.6.4 · 0cb411fc90</footer>
  <script id="markdown-source" type="text/plain">${htmlMarkdown}
</script>
  <script src="../../assets/guide.js"></script>
</body>
</html>
`;

fs.writeFileSync(outputPath, html, 'utf8');
console.log(`Generated ${path.relative(root, outputPath)}`);
