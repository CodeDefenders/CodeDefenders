import fs from 'fs';
import path from 'path';

function walk(dir) {
  const out = [];
  const entries = fs.readdirSync(dir, {withFileTypes: true});
  for (const e of entries) {
    const p = path.join(dir, e.name);
    if (e.isDirectory()) out.push(...walk(p));
    else out.push(p);
  }
  return out;
}

function extractI18nStrings(content) {
    const results = [];
    // inspired by: https://blog.stevenlevithan.com/archives/match-quoted-string
    const regExp = /i18n\.tr\s*\(\s*((['"`])(?:\\.|(?!\2).)*\2)/g;
    let match;
    while ((match = regExp.exec(content)) !== null) {
        let text = match[1].slice(1, -1);

    // no unescaping done, as it's inserted into JS code again later anyway.

    if (text.length) {
      results.push(text);
    }
  }
  return results;
}

export function i18nCollectPlugin(options = {}) {
  const {
    roots = ['src'],
    exts = new Set(['.js', '.mjs']),
    output = 'src/main/resources/i18n.js.json'
  } = options;

  return {
    name: 'i18n-collect',
    buildStart() {
      // ensure output dir exists
      const outDir = path.dirname(output);
      if (!fs.existsSync(outDir)) {
        fs.mkdirSync(outDir, {recursive: true});
      }
    },
    generateBundle() {
      const stringSet = new Set();

      for (const root of roots) {
        if (!fs.existsSync(root)) continue;
        for (const file of walk(root)) {
          if (!exts.has(path.extname(file))) continue;

          const text = fs.readFileSync(file, 'utf8');
          const strings = extractI18nStrings(text);
          for (const s of strings) {
            stringSet.add(s);
          }
        }
      }

      const o = {strings: Array.from(stringSet)};
      const json = JSON.stringify(o, null, 4);
      fs.writeFileSync(output, json, 'utf8');
    }
  };
}
