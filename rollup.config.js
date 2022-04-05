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
/* Warning: This directory is cleaned before build. */
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


/* Bundle Config.
----------------------------------------------------------------------------- */

/* Maps imports in our code to globals provided by the libraries. */
const GLOBALS = {
    [path.resolve(__dirname, `${SRC_DIR}/main`)]: 'CodeDefenders',
    [path.resolve(__dirname, `${SRC_DIR}/thirdparty/bootstrap`)]: 'bootstrap',
    [path.resolve(__dirname, `${SRC_DIR}/thirdparty/codemirror`)]: 'CodeMirror',
    [path.resolve(__dirname, `${SRC_DIR}/thirdparty/datatables`)]: 'DataTable',
    [path.resolve(__dirname, `${SRC_DIR}/thirdparty/jquery`)]: 'jQuery'
};
const IS_IN_GLOBALS = function (id, parentId, isResolved) {
    const fullPath = path.resolve(parentId, '..', id);
    return GLOBALS.hasOwnProperty(fullPath);
};

/*
 * Init bundle: Contains code that is run on every page load.
 * Runs the code from 'init/index.js'.
 */
const initConfig = {
    input: `${SRC_DIR}/init/index.js`,
    plugins: PLUGINS,
    external: IS_IN_GLOBALS,
    output: {
        file: `${BUILD_DIR}/codedefenders_init.js`,
        name: 'CodeDefenders',
        format: 'iife',
        sourcemap: true,
        globals: GLOBALS,
        extend: true
    }
};

/*
 * Main bundle: Contains code that is useful outside of games.
 * Creates a global 'CodeDefenders' variable with the exports of 'main/index.js'.
 */
const mainConfig = {
    input: `${SRC_DIR}/main/index.js`,
    plugins: PLUGINS,
    external: IS_IN_GLOBALS,
    output: {
        file: `${BUILD_DIR}/codedefenders_main.js`,
        name: 'CodeDefenders',
        format: 'iife',
        sourcemap: true,
        globals: GLOBALS,
        extend: true
    }
};

/*
 * Game bundle: Contains code for game components.
 * Extends the main bundle's global 'CodeDefenders' variable with the exports of 'game/index.js'.
 */
const gameConfig = {
    input: `${SRC_DIR}/game/index.js`,
    plugins: PLUGINS,
    external: IS_IN_GLOBALS,
    output: {
        file: `${BUILD_DIR}/codedefenders_game.js`,
        name: 'CodeDefenders',
        format: 'iife',
        sourcemap: true,
        globals: GLOBALS,
        extend: true
    }
};

/*
 * Bundle JS libraries ourself. At the moment, the only advantage will be that we can minify them ourselves,
 * but later it will help us import them as modules. */
const thirdpartyConfigs = [
    ['bootstrap', 'bootstrap'],
    ['codemirror', 'CodeMirror'],
    ['datatables', 'DataTable'],
    ['jquery', 'jquery']
].map(([id, globalName]) => {
    return {
        input: `${SRC_DIR}/thirdparty/${id}.js`,
        plugins: PLUGINS,
        output: {
            file: `${BUILD_DIR}/${id}.js`,
            name: globalName,
            format: 'iife',
            sourcemap: true
        }
    }
})

const bundleConfig = [
    initConfig,
    mainConfig,
    gameConfig,
    ...thirdpartyConfigs
];


/* Module Config.
 *
 * This will require a bit more refactoring to work properly. Switching to modules
 * will enable us to import dependencies from scripts instead of including them manually,
 * and they will be loaded async.
 *
 * For example:
 * <script type="module">
 *     import {GameChat} from './js/codedefenders_game.mjs';
 *     import CodeMirror from './js/codemirror.mjs';
 * </script>
----------------------------------------------------------------------------- */

const moduleConfig = {
    input: [
        `${SRC_DIR}/game/index.js`,
        `${SRC_DIR}/main/index.js`,
        `${SRC_DIR}/init/index.js`,
        `${SRC_DIR}/thirdparty/bootstrap.js`,
        `${SRC_DIR}/thirdparty/codemirror.js`,
        `${SRC_DIR}/thirdparty/datatables.js`,
        `${SRC_DIR}/thirdparty/jquery.js`,
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

    /* Get latest timestamp of source files and this config file. */
    let latestSrcTimestamp = null;
    for (const sourceFile of [...walkSync(SRC_DIR), __filename]) {
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
    fs.rmSync(BUILD_DIR, { recursive: true, force: true });
}


export default bundleConfig;
