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
    const re = /i18n\.tr\s*\(\s*((['"`])(?:\\.|(?!\2).)*\2)/g;
    let m;
    while ((m = re.exec(content)) !== null) {
        const literal = m[1];
        const quote = literal[0];
        let text = literal.slice(1, -1);

        /*
         * Alternative approach for unescaping:
         * ```
         * const unescapedString = eval('"' + text + '"');
         * ```
         * Causes warning: (!) Use of eval is strongly discouraged
         */

        // Unescape the enclosing quote type dynamically
        text = text.replace(new RegExp('\\\\' + quote, 'g'), quote);

        // Unescape common sequences and backslashes
        text = text
            .replace(/\\n/g, '\n')
            .replace(/\\r/g, '\r')
            .replace(/\\t/g, '\t')
            .replace(/\\\\/g, '\\');

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
            /*
             * bundle name -> translation strings
             * @type {Map<string, Set<String>>}
             */
            const buckets = new Map();

            for (const root of roots) {
                if (!fs.existsSync(root)) continue;
                for (const file of walk(root)) {
                    if (!exts.has(path.extname(file))) continue;
                    const folderName = path.basename(path.dirname(file));
                    const text = fs.readFileSync(file, 'utf8');
                    const strings = extractI18nStrings(text);
                    if (!strings.length) continue;

                    let set = buckets.get(folderName);
                    if (set === undefined) {
                        set = new Set();
                        buckets.set(folderName, set);
                    }
                    for (const s of strings) {
                        set.add(s);
                    }
                }
            }

            const o = {};
            for (const [dir, set] of buckets.entries()) {
                o[dir] = Array.from(set);
            }
            const json = JSON.stringify(o, null, 4);
            fs.writeFileSync(output, json, 'utf8');
        }
    };
}
