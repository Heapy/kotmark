#!/usr/bin/env node
// Kotlin Toolchain 0.12 exposes JS test linking, but has no JS test runner.
import { readdirSync } from 'node:fs';
import { spawnSync } from 'node:child_process';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { resolve } from 'node:path';

const root = fileURLToPath(new URL('..', import.meta.url));
const modules = ['kotmark', ...readdirSync(root).filter(name => name.startsWith('commonmark-ext-')).sort()];
const build = spawnSync('./kotlin', ['task', ...modules.map(name => `:${name}:linkJsTest`)], {
  cwd: root, stdio: 'inherit',
});
if (build.error) throw build.error;
if (build.status !== 0) process.exit(build.status ?? 1);

const suites = [];
const tests = [];
let skipped = 0;
globalThis.describe = (name, body) => {
  suites.push(name);
  try { body(); } finally { suites.pop(); }
};
globalThis.xdescribe = () => { skipped++; };
globalThis.it = (name, body) => { tests.push({ name: [...suites, name].join('.'), body }); };
globalThis.xit = () => { skipped++; };
for (const name of modules) {
  const entry = resolve(root, `build/artifacts/CompiledWebArtifact/${name}jsTestrelease/kotlin-output/${name}_test.mjs`);
  await import(pathToFileURL(entry).href);
}
let failed = 0;
for (const { name, body } of tests) {
  try {
    await body();
  } catch (error) {
    failed++;
    console.error(`FAIL ${name}`, error);
  }
}
console.log(`JS: ${tests.length - failed} passed, ${failed} failed, ${skipped} skipped tests/suites`);
if (tests.length === 0 || failed !== 0) process.exitCode = 1;
