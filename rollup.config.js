import path from 'path';
import fs from 'fs';

import resolve from '@rollup/plugin-node-resolve';
import commonjs from '@rollup/plugin-commonjs';
import babel from '@rollup/plugin-babel';
import {terser} from 'rollup-plugin-terser';
import replace from '@rollup/plugin-replace';


/* Constants.
----------------------------------------------------------------------------- */

/* Enable to clean the build folder. */
const CLEAN_BUILD = true;
/* Enable to generate builds without minification. */
const MINIMIZE_OUTPUT = true;
/* Enable to build even when no files have changed. */
const FORCE_BUILD = false;

const SRC_DIR = 'src/main/javascript';
/* WARNING: This directory is deleted before build. */
const BUILD_DIR = 'target/rollup/js';

const PLUGINS = [
    /* Resolve imports to npm dependencies. */
    resolve(),
    /* Needed to make Popper (Bootstrap dependency) work. Taken from Bootstrap's rollup config. */
    replace({
        'process.env.NODE_ENV': '"production"',
        preventAssignment: true
    }),
    /* Interface with other JS module types (CommonJS, AMD, UMD). */
    commonjs(),
    /* Transpile to browser-friendly JS. */
    babel({
        babelHelpers: 'bundled',
        presets: ['@babel/preset-env']
    }),
    /* Minimize output. */
    MINIMIZE_OUTPUT && terser()
];


/* Module Config.
----------------------------------------------------------------------------- */

const moduleConfig = {
    input: [
        `${SRC_DIR}/game/index.js`,
        `${SRC_DIR}/main/index.js`,
        `${SRC_DIR}/init/index.js`,
        ...fs.readdirSync(`${SRC_DIR}/thirdparty`)
                .map(name => `${SRC_DIR}/thirdparty/${name}`)
    ],
    plugins: PLUGINS,
    output: {
        dir: BUILD_DIR,
        format: 'esm',
        sourcemap: true,
        extend: true,
        entryFileNames: function (chunkInfo) {
            const manualChunkNames = {
                [path.resolve(__dirname, `${SRC_DIR}/game/index.js`)]: 'codedefenders_game',
                [path.resolve(__dirname, `${SRC_DIR}/main/index.js`)]: 'codedefenders_main',
                [path.resolve(__dirname, `${SRC_DIR}/init/index.js`)]: 'codedefenders_init'
            };
            if (manualChunkNames.hasOwnProperty(chunkInfo.facadeModuleId)) {
                return `${manualChunkNames[chunkInfo.facadeModuleId]}.mjs`;
            } else {
                return `${chunkInfo.name}.mjs`;
            }
        },
        chunkFileNames: 'chunk-[hash].mjs',
    }
};


/* Check timestamps to avoid unnecessary builds.
----------------------------------------------------------------------------- */

/* From https://stackoverflow.com/a/63111390 */
const walkSync = function* (dir) {
    const files = fs.readdirSync(dir, { withFileTypes: true });
    for (const file of files) {
        if (file.isDirectory()) {
            yield* walkSync(path.join(dir, file.name));
        } else {
            yield path.join(dir, file.name);
        }
    }
};

const shouldSkipBuild = function () {
    if (!fs.existsSync(SRC_DIR) || !fs.existsSync(BUILD_DIR)) {
        return false;
    }

    /* Get latest timestamp of source files, this config file and the package.json. */
    let latestSrcTimestamp = null;
    for (const sourceFile of [...walkSync(SRC_DIR), __filename, 'package.json']) {
        let stats = fs.statSync(sourceFile);
        if (latestSrcTimestamp === null
                || stats.mtime.getTime() > latestSrcTimestamp.getTime())  {
            latestSrcTimestamp = stats.mtime;
        }
    }

    /* Get earliest timestamp of build files. */
    let earliestBuildTimestamp = null;
    for (const sourceFile of walkSync(BUILD_DIR)) {
        let stats = fs.statSync(sourceFile);
        if (earliestBuildTimestamp === null
                || stats.mtime.getTime() < earliestBuildTimestamp.getTime())  {
            earliestBuildTimestamp = stats.mtime;
        }
    }

    if (latestSrcTimestamp === null || earliestBuildTimestamp === null) {
        return false;
    }

    /* Only skip build when all build files are newer than src files. */
    return latestSrcTimestamp.getTime() < earliestBuildTimestamp.getTime();
};

if (!FORCE_BUILD && shouldSkipBuild()) {
    console.log('No source files changed, skipping build.')
    process.exit(0);
}


/* Delete build folder. */
if (CLEAN_BUILD) {
    console.log(`Deleting build folder '${BUILD_DIR}'.`);
    fs.rmSync(BUILD_DIR, { recursive: true, force: true });
}


export default moduleConfig;
