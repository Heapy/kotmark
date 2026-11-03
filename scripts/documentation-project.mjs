import { cpSync, mkdirSync, readFileSync, readdirSync, rmSync, writeFileSync } from 'node:fs';
import { resolve } from 'node:path';

// The Dokka adapter cannot model Android or the JVM/Android shared source alias.
// Build a disposable documentation model; never patch the locked plugin or production sources.
export function prepareDocumentationProject(root, libraries) {
  const project = resolve(root, 'build/documentation-project');
  mkdirSync(project, { recursive: true });
  const modules = [...libraries, 'test-fixtures'];
  for (const module of modules) {
    const original = resolve(root, module);
    const destination = resolve(project, module);
    rmSync(destination, { recursive: true, force: true });
    mkdirSync(destination, { recursive: true });
    let yaml = readFileSync(resolve(original, 'module.yaml'), 'utf8');
    let foundPlatforms = false;
    yaml = yaml.replace(/platforms:\s*\[([^\]]+)\]/, (_, list) => {
      foundPlatforms = true;
      return `platforms: [${list.split(',').map(p => p.trim()).filter(p => p !== 'android').join(', ')}]`;
    });
    if (!foundPlatforms) throw new Error(`Expected explicit KMP platforms in ${module}/module.yaml`);
    yaml = yaml.replace(/^aliases:\s*\n  - jvmAndAndroid: \[jvm, android\]\s*\n/m, '');
    if (/^aliases:/m.test(yaml)) throw new Error(`Review new aliases in ${module} for documentation`);
    writeFileSync(resolve(destination, 'module.yaml'), yaml);
    for (const entry of readdirSync(original, { withFileTypes: true })) {
      if (!entry.isDirectory() || !/^(src|resources)(@.+)?$/.test(entry.name) || entry.name.endsWith('@android')) continue;
      const name = entry.name.replace('@jvmAndAndroid', '@jvm');
      cpSync(resolve(original, entry.name), resolve(destination, name), {
        recursive: true, force: false, errorOnExist: true,
      });
    }
  }
  for (const name of ['kotlin', 'kotlin.bat', 'common.module-template.yaml', 'library.module-template.yaml']) {
    cpSync(resolve(root, name), resolve(project, name));
  }
  // These are disposable copies of downloaded dependencies, all inside ignored build/.
  const plugins = ['bcv', 'benchmark', 'dokka', 'kover'];
  for (const name of plugins) {
    const destination = resolve(project, 'plugins', name);
    rmSync(destination, { recursive: true, force: true });
    cpSync(resolve(root, 'plugins', name), destination, { recursive: true });
  }
  writeFileSync(resolve(project, 'project.yaml'),
    'modules:\n' + [...modules, ...plugins.map(name => `plugins/${name}`)].sort().map(name => `  - ${name}\n`).join('') +
    'plugins:\n' + plugins.map(name => `  - //plugins/${name}\n`).join(''));
  return ['--project-dir', project, '--build-dir', resolve(root, 'build/documentation')];
}
