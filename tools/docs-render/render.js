// Renders docs/diseno-mvp.md into a themed HTML artifact page.
const fs = require('fs');
const path = require('path');
const { marked } = require('marked');

const [,, srcPath, outPath] = process.argv;
const md = fs.readFileSync(srcPath, 'utf8');

const slug = s => s.toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '')
  .replace(/<[^>]+>/g, '').replace(/[^a-z0-9]+/g, '-').replace(/(^-|-$)/g, '');

const toc = [];
const renderer = new marked.Renderer();
renderer.heading = function ({ tokens, depth }) {
  const text = this.parser.parseInline(tokens);
  const id = slug(text);
  if (depth === 2) toc.push({ id, text });
  return `<h${depth} id="${id}">${text}</h${depth}>\n`;
};
renderer.code = function ({ text, lang }) {
  if (lang === 'mermaid') return `<div class="diagram"><pre class="mermaid">\n${text}\n</pre></div>\n`;
  const esc = text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
  return `<pre class="code" data-lang="${lang || ''}"><code>${esc}</code></pre>\n`;
};
renderer.table = function ({ header, rows }) {
  const cell = (c, tag) => `<${tag}${c.align ? ` style="text-align:${c.align}"` : ''}>${this.parser.parseInline(c.tokens)}</${tag}>`;
  const head = `<tr>${header.map(c => cell(c, 'th')).join('')}</tr>`;
  const body = rows.map(r => `<tr>${r.map(c => cell(c, 'td')).join('')}</tr>`).join('\n');
  return `<div class="table-wrap"><table><thead>${head}</thead><tbody>${body}</tbody></table></div>\n`;
};
marked.use({ renderer, gfm: true });

// Strip the H1 and the version line; they go into the page header.
const lines = md.split('\n');
const title = lines[0].replace(/^#\s*/, '');
const meta = lines[2];
const body = marked.parse(lines.slice(3).join('\n'));

const tocHtml = toc.map(t => `<li><a href="#${t.id}">${t.text}</a></li>`).join('\n');

const html = `<title>Diseño MVP Seguimiento Metabólico</title>
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Source+Serif+4:opsz,wght@8..60,500;8..60,600&family=Source+Sans+3:wght@400;500;600&family=JetBrains+Mono:wght@400;500&display=swap">
<style>
:root {
  --bg: #f5f7f6; --surface: #ffffff; --ink: #1b2226; --muted: #5b676d; --rule: #d9dedc; --rule-soft: #e8ecea;
  --accent: #0f6e73; --accent-ink: #0a5256; --accent-soft: #dcecec;
  --amber: #b9741e; --amber-soft: #f6ead6;
  --code-bg: #eef2f1; --quote-bar: #0f6e73;
  --serif: 'Source Serif 4', Georgia, 'Times New Roman', serif;
  --sans: 'Source Sans 3', 'Helvetica Neue', Arial, sans-serif;
  --mono: 'JetBrains Mono', ui-monospace, SFMono-Regular, Menlo, monospace;
}
@media (prefers-color-scheme: dark) {
  :root:not([data-theme="light"]) {
    --bg: #131a1d; --surface: #1a2226; --ink: #e6ebe9; --muted: #98a4a9; --rule: #2a3438; --rule-soft: #222c30;
    --accent: #4fb3b8; --accent-ink: #7fcfd2; --accent-soft: #163b3d;
    --amber: #e0a150; --amber-soft: #3a2c16;
    --code-bg: #101619; --quote-bar: #4fb3b8;
  }
}
:root[data-theme="dark"] {
  --bg: #131a1d; --surface: #1a2226; --ink: #e6ebe9; --muted: #98a4a9; --rule: #2a3438; --rule-soft: #222c30;
  --accent: #4fb3b8; --accent-ink: #7fcfd2; --accent-soft: #163b3d;
  --amber: #e0a150; --amber-soft: #3a2c16;
  --code-bg: #101619; --quote-bar: #4fb3b8;
}
* { box-sizing: border-box; }
body { margin: 0; background: var(--bg); color: var(--ink); font-family: var(--sans); font-size: 17px; line-height: 1.55; }
a { color: var(--accent-ink); text-decoration: none; border-bottom: 1px solid var(--accent-soft); }
a:hover, a:focus-visible { border-bottom-color: var(--accent); outline: none; }
a:focus-visible { outline: 2px solid var(--accent); outline-offset: 2px; }

.page { display: grid; grid-template-columns: 1fr; max-width: 1240px; margin: 0 auto; padding: 0 20px 96px; }
@media (min-width: 1100px) { .page { grid-template-columns: 240px minmax(0, 760px); column-gap: 56px; } }

header.doc { grid-column: 1 / -1; padding: 56px 0 28px; border-bottom: 1px solid var(--rule); margin-bottom: 40px; }
header.doc .eyebrow { font-family: var(--mono); font-size: 12px; letter-spacing: 0.08em; text-transform: uppercase; color: var(--accent-ink); margin: 0 0 14px; }
header.doc h1 { font-family: var(--serif); font-weight: 600; font-size: clamp(30px, 4.2vw, 44px); line-height: 1.12; margin: 0 0 14px; text-wrap: balance; max-width: 20ch; }
header.doc .meta { color: var(--muted); margin: 0; font-size: 15px; }
header.doc .legend { display: flex; flex-wrap: wrap; gap: 8px 18px; margin: 22px 0 0; padding: 0; list-style: none; font-size: 14px; color: var(--muted); }
header.doc .legend li::before { content: ''; display: inline-block; width: 10px; height: 10px; border-radius: 2px; margin-right: 7px; vertical-align: -1px; background: var(--accent); }
header.doc .legend li.est::before { background: var(--amber); }
header.doc .legend li.inf::before { background: transparent; border: 2px solid var(--amber); width: 6px; height: 6px; }

nav.toc { display: none; }
@media (min-width: 1100px) {
  nav.toc { display: block; position: sticky; top: 28px; align-self: start; max-height: calc(100vh - 56px); overflow-y: auto; padding-right: 8px; }
  nav.toc h2 { font-family: var(--mono); font-size: 11px; letter-spacing: 0.1em; text-transform: uppercase; color: var(--muted); margin: 0 0 12px; font-weight: 500; }
  nav.toc ol { list-style: none; margin: 0; padding: 0; border-left: 1px solid var(--rule); }
  nav.toc li a { display: block; padding: 5px 0 5px 14px; margin-left: -1px; border-left: 2px solid transparent; border-bottom: 0; font-size: 14px; line-height: 1.35; color: var(--muted); }
  nav.toc li a:hover { color: var(--ink); border-left-color: var(--rule); }
}

main { min-width: 0; }
main h2 { font-family: var(--serif); font-weight: 600; font-size: 28px; line-height: 1.2; margin: 64px 0 18px; padding-top: 18px; border-top: 1px solid var(--rule); text-wrap: balance; }
main h2:first-child { margin-top: 0; border-top: 0; padding-top: 0; }
main h3 { font-family: var(--sans); font-weight: 600; font-size: 19px; margin: 34px 0 10px; text-wrap: balance; }
main p { margin: 0 0 16px; max-width: 68ch; }
main ul, main ol { margin: 0 0 18px; padding-left: 24px; max-width: 68ch; }
main li { margin: 0 0 6px; }
main li p { margin: 0; }
main hr { border: 0; height: 0; margin: 0; }
main strong { font-weight: 600; }
main code { font-family: var(--mono); font-size: 0.86em; background: var(--code-bg); padding: 1px 5px; border-radius: 3px; }
main pre.code { background: var(--code-bg); border: 1px solid var(--rule-soft); border-radius: 6px; padding: 16px 18px; overflow-x: auto; margin: 0 0 22px; font-size: 13.5px; line-height: 1.5; position: relative; }
main pre.code code { background: transparent; padding: 0; font-size: inherit; }
main pre.code[data-lang]:not([data-lang=""])::before { content: attr(data-lang); position: absolute; top: 8px; right: 12px; font-family: var(--mono); font-size: 10.5px; letter-spacing: 0.08em; text-transform: uppercase; color: var(--muted); }
main blockquote { margin: 0 0 22px; padding: 14px 20px; border-left: 3px solid var(--quote-bar); background: var(--surface); border-radius: 0 6px 6px 0; max-width: 68ch; }
main blockquote p { margin: 0 0 8px; }
main blockquote p:last-child { margin: 0; }

.table-wrap { overflow-x: auto; margin: 0 0 24px; border: 1px solid var(--rule); border-radius: 6px; background: var(--surface); }
table { border-collapse: collapse; width: 100%; font-size: 15px; line-height: 1.4; }
th, td { text-align: left; vertical-align: top; padding: 10px 14px; border-bottom: 1px solid var(--rule-soft); }
th { font-family: var(--mono); font-size: 11.5px; letter-spacing: 0.06em; text-transform: uppercase; color: var(--muted); font-weight: 500; background: var(--code-bg); white-space: nowrap; }
tr:last-child td { border-bottom: 0; }
td code { white-space: nowrap; }
td { font-variant-numeric: tabular-nums; }

.diagram { margin: 0 0 26px; padding: 18px; border: 1px solid var(--rule); border-radius: 6px; background: var(--surface); overflow-x: auto; }
.diagram pre.mermaid { margin: 0; font-family: var(--mono); font-size: 13px; }

@media (prefers-reduced-motion: reduce) { * { scroll-behavior: auto !important; } }
html { scroll-behavior: smooth; }
</style>

<div class="page">
  <header class="doc">
    <p class="eyebrow">Documento de diseño · MVP</p>
    <h1>${title.replace(' — Diseño del MVP', '')}</h1>
    <p class="meta">${meta}</p>
    <ul class="legend">
      <li>Observación: lecturas medidas, texto del usuario</li>
      <li class="est">Estimación: nutrientes con rango y confianza</li>
      <li class="inf">Inferencia: patrones con n, calidad y confusores</li>
    </ul>
  </header>
  <nav class="toc" aria-label="Secciones">
    <h2>Secciones</h2>
    <ol>
${tocHtml}
    </ol>
  </nav>
  <main>
${body}
  </main>
</div>
`;
fs.writeFileSync(outPath, html);
console.log('wrote', outPath, (html.length / 1024).toFixed(0) + ' KB', 'sections:', toc.length);
