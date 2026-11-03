#!/usr/bin/env node
// Serialize plugins that launch nested Toolchain builds to keep memory use bounded.
import { readdirSync, readFileSync } from 'node:fs';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { resolve } from 'node:path';

const root = fileURLToPath(new URL('..', import.meta.url));
const commands = {
  'api-check': [['check', 'apiCheck', 'klibApiCheck']],
  'api-dump': [['do', 'apiDump'], ['do', 'klibApiDump']],
  'jvm-api-check': [['check', 'apiCheck']],
  'jvm-api-dump': [['do', 'apiDump']],
  'klib-api-check': [['check', 'klibApiCheck']],
  'klib-api-dump': [['do', 'klibApiDump']],
};
const [command, ...requestedModules] = process.argv.slice(2);
if (!Object.hasOwn(commands, command)) {
  throw new Error(`Usage: node scripts/library-tools.mjs <${Object.keys(commands).join('|')}> [module ...]`);
}
const libraries = readdirSync(root, { withFileTypes: true })
  .filter(entry => entry.isDirectory())
  .map(entry => entry.name)
  .filter(name => {
    try {
      return readFileSync(resolve(root, name, 'module.yaml'), 'utf8').includes('//library.module-template.yaml');
    } catch (error) {
      if (error.code === 'ENOENT') return false;
      throw error;
    }
  }).sort();
const modules = requestedModules.length ? requestedModules : libraries;
if (!modules.length || modules.some(name => !libraries.includes(name))) {
  throw new Error(`Select library modules from: ${libraries.join(', ')}`);
}
for (const module of modules) {
  for (const args of commands[command]) {
    console.log(`${module}: ${args.join(' ')}`);
    const windows = process.platform === 'win32';
    const executable = windows ? 'cmd.exe' : './kotlin';
    const prefix = windows ? ['/d', '/c', 'kotlin.bat'] : [];
    const result = spawnSync(executable, [...prefix, ...args, '-m', module], { cwd: root, stdio: 'inherit' });
    if (result.error) throw result.error;
    if (result.status !== 0) process.exit(result.status ?? 1);
  }
}
