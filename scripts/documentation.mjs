#!/usr/bin/env node
import { discoverModules, selectShard } from './ci-model.mjs';
import { runToolchain } from './ci-process.mjs';

const args = process.argv.slice(2);
if (args.length > 1) throw new Error('Usage: node scripts/documentation.mjs [INDEX/COUNT]');
const modules = selectShard(discoverModules().filter(m => m.library).map(m => m.name), args[0]);
// Bound Dokka JVM memory; its nested model build shares dependencies across modules.
for (const module of modules) runToolchain(['do', 'dokkaHtml', '-m', module, '--build-dir', 'build/documentation']);
