#!/usr/bin/env node
import { resolve } from 'node:path';
import { discoverModules, root } from './ci-model.mjs';
import { runToolchain } from './ci-process.mjs';

const commands = {
  'api-check': [['check', 'apiCheck', 'klibApiCheck']],
  'api-dump': [['do', 'apiDump'], ['do', 'klibApiDump']],
  'jvm-api-check': [['check', 'apiCheck']],
  'jvm-api-dump': [['do', 'apiDump']],
  'klib-api-check': [['check', 'klibApiCheck']],
  'klib-api-dump': [['do', 'klibApiDump']],
};
const [command, ...args] = process.argv.slice(2);
if (!Object.hasOwn(commands, command)) throw new Error(`Usage: library-tools.mjs <${Object.keys(commands).join('|')}> [--targets TARGET,...] [module ...]`);
const requestedModules = [];
let targets;
for (let index = 0; index < args.length; index++) {
  if (args[index] === '--targets') {
    if (targets || !args[index + 1]) throw new Error('Supply --targets once with a nonempty comma-separated list');
    targets = args[++index].split(',');
    if (targets.some(t => !/^[A-Za-z][A-Za-z0-9]*$/.test(t)) || new Set(targets).size !== targets.length) throw new Error('Invalid or duplicate targets');
  } else requestedModules.push(args[index]);
}
const libraries = discoverModules().filter(m => m.library);
const modules = requestedModules.length ? [...new Set(requestedModules)] : libraries.map(m => m.name);
if (!modules.length || modules.some(name => !libraries.some(m => m.name === name))) throw new Error(`Select library modules from: ${libraries.map(m => m.name).join(', ')}`);
const env = { ...process.env };
if (targets) {
  const supported = new Set(libraries.flatMap(m => m.platforms).filter(p => !['android', 'watchosDeviceArm64'].includes(p)));
  const host = target => ['jvm', 'js', 'wasmJs', 'wasmWasi'].includes(target) ||
    (process.platform === 'darwin' ? /^(macos|ios|tvos|watchos)/.test(target) : process.platform === 'win32' ? target === 'mingwX64' : /^linux/.test(target));
  if (targets.some(t => !supported.has(t) || !host(t))) throw new Error(`Unsupported ABI targets on ${process.platform}: ${targets}`);
  if (command.startsWith('jvm-') && targets.some(t => t !== 'jvm') || command.startsWith('klib-') && targets.includes('jvm')) throw new Error('Command and target selection disagree');
  env.KTC_BCV_TARGETS = targets.join(',');
  env.KTC_BCV_COMPILATION_DIR = resolve(root, 'build/bcv-workspace/compile');
  const compilationTask = target => target === 'jvm' ? 'jarJvm' :
    `compile${target[0].toUpperCase()}${target.slice(1)}${['js', 'wasmJs', 'wasmWasi'].includes(target) ? '' : 'Debug'}`;
  const tasks = libraries.filter(m => modules.includes(m.name)).flatMap(m => targets.filter(t => m.platforms.includes(t)).map(t => `:${m.name}:${compilationTask(t)}`));
  if (!tasks.length) throw new Error('Selected modules do not declare any requested ABI target');
  runToolchain(['task', ...tasks, '--build-dir', env.KTC_BCV_COMPILATION_DIR], { env, label: `Compile ABI artifacts (${targets.join(', ')})` });
}
// Bound reader JVM memory; dependency compilation and reader binaries are shared.
for (const module of modules) {
  for (const args of commands[command]) runToolchain([...args, '-m', module], { env, label: `${module}: ${args.join(' ')}` });
}
