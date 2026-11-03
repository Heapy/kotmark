import { readdirSync, readFileSync, existsSync } from 'node:fs';
import { resolve } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

export const root = fileURLToPath(new URL('..', import.meta.url));
export function discoverModules(directory = root) {
  return readdirSync(directory, { withFileTypes: true })
    .filter(entry => entry.isDirectory() && existsSync(resolve(directory, entry.name, 'module.yaml')))
    .map(entry => {
      const text = readFileSync(resolve(directory, entry.name, 'module.yaml'), 'utf8');
      const platforms = text.match(/platforms:\s*\[([^\]]+)\]/s)?.[1].split(',').map(p => p.trim()) ?? [];
      const library = text.includes('//library.module-template.yaml');
      if (library && !platforms.length) throw new Error(`Cannot discover platforms for ${entry.name}`);
      return { name: entry.name, platforms, library,
        tests: readdirSync(resolve(directory, entry.name), { withFileTypes: true })
          .some(item => item.isDirectory() && /^test(?:@.+)?$/.test(item.name)) };
    }).sort((a, b) => a.name.localeCompare(b.name));
}

export function selectShard(modules, shard = '1/1') {
  if (!/^[1-9]\d*\/[1-9]\d*$/.test(shard)) throw new Error('Shard must be INDEX/COUNT (one-based)');
  const [index, count] = shard.split('/').map(Number);
  if (index > count || count > modules.length) throw new Error(`Invalid shard ${shard}`);
  const priority = ['kotmark', 'autolink', 'kotmark-renderer-kotlinx-html'];
  const ordered = [...priority.filter(name => modules.includes(name)), ...modules.filter(name => !priority.includes(name)).sort()];
  return ordered.filter((_, position) => position % count === index - 1);
}

if (process.argv[1] && import.meta.url === pathToFileURL(resolve(process.argv[1])).href) {
  const [command, argument] = process.argv.slice(2);
  const modules = discoverModules();
  if (command === 'libraries') console.log(selectShard(modules.filter(m => m.library).map(m => m.name), argument).join(' '));
  else if (command === 'tests') {
    const selected = modules.filter(m => m.tests && m.platforms.includes(argument));
    if (!selected.length) throw new Error(`No test modules for ${argument}`);
    console.log(selected.map(m => m.name).join(' '));
  } else throw new Error('Usage: node scripts/ci-model.mjs libraries [INDEX/COUNT] | tests PLATFORM');
}
