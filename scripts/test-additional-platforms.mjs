#!/usr/bin/env node
// KTC 0.13 gaps: WASI, tvOS/watchOS simulators, and device compilation without linking tests.
import { readdirSync, readFileSync } from 'node:fs';
import { spawnSync } from 'node:child_process';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { resolve } from 'node:path';

const root = fileURLToPath(new URL('..', import.meta.url));
const args = process.argv.slice(2);
const compileOnly = args[0] === '--compile-only';
const linkOnly = args[0] === '--link-only';
const platforms = compileOnly || linkOnly ? args.slice(1) : args;
const [platform] = platforms;
const families = { tvosSimulatorArm64: 'tvOS', watchosSimulatorArm64: 'watchOS' };
const devicePlatforms = ['iosArm64', 'tvosArm64', 'watchosArm64', 'watchosDeviceArm64'];
const validPlatforms = compileOnly
  ? platforms.length > 0 && platforms.every(p => devicePlatforms.includes(p))
  : platforms.length === 1 && (linkOnly ? platform === 'linuxArm64' : platform === 'wasmWasi' || platform in families);
if (!validPlatforms) {
  throw new Error('Expected wasmWasi|tvosSimulatorArm64|watchosSimulatorArm64, --compile-only <Apple device targets>, or --link-only linuxArm64');
}
function modulesFor(platform) {
  const modules = readdirSync(root, { withFileTypes: true })
    .filter(entry => entry.isDirectory())
    .map(entry => entry.name)
    .filter(name => {
      try {
        const yaml = readFileSync(resolve(root, name, 'module.yaml'), 'utf8');
        const supportsPlatform = yaml.match(/platforms:\s*\[([^\]]+)\]/s)?.[1].split(',').map(p => p.trim()).includes(platform);
        // Modules without tests are compiled through their dependents rather
        // than run as empty test executables.
        const hasTests = readdirSync(resolve(root, name), { withFileTypes: true })
          .some(entry => entry.isDirectory() && /^test(?:@.+)?$/.test(entry.name));
        return supportsPlatform && hasTests;
      } catch (error) {
        if (error.code === 'ENOENT') return false;
        throw error;
      }
    }).sort();
  if (!modules.length) throw new Error(`No modules declare ${platform}`);
  return modules;
}

function run(command, args, capture = false) {
  const result = spawnSync(command, args, {
    cwd: root, encoding: 'utf8', stdio: capture ? 'pipe' : 'inherit', maxBuffer: 64 * 1024 * 1024,
  });
  if (result.error) throw result.error;
  if (result.status !== 0) {
    if (capture) process.stderr.write((result.stdout ?? '') + (result.stderr ?? ''));
    throw new Error(`${command} failed: ${result.status ?? result.signal}`);
  }
  return result.stdout;
}

const taskSuffix = platform => platform[0].toUpperCase() + platform.slice(1);
if (compileOnly) {
  // compile*TestDebug also compiles production sources without linking device
  // executables that cannot run on hosted CI machines.
  run('./kotlin', ['task', ...platforms.flatMap(p => modulesFor(p)
    .map(name => `:${name}:compile${taskSuffix(p)}TestDebug`))]);
  console.log(`Compiled production and test sources for ${platforms.join(', ')}`);
  process.exit(0);
}

const modules = modulesFor(platform);
const suffix = taskSuffix(platform);
run('./kotlin', ['task', ...modules.map(name => `:${name}:link${suffix}TestDebug`)]);
if (linkOnly) {
  console.log(`Linked ${modules.length} ${platform} test executables for execution on a matching host`);
  process.exit(0);
}

let device;
let bootedHere = false;
try {
  if (platform in families) {
    const { devices } = JSON.parse(run('xcrun', ['simctl', 'list', 'devices', 'available', '--json'], true));
    device = Object.entries(devices)
      .filter(([runtime]) => runtime.includes(`.${families[platform]}-`))
      .flatMap(([, entries]) => entries)
      .find(entry => entry.isAvailable);
    if (!device) throw new Error(`No available ${families[platform]} simulator; install its Xcode runtime`);
    if (device.state !== 'Booted') {
      run('xcrun', ['simctl', 'boot', device.udid]);
      bootedHere = true;
    }
    run('xcrun', ['simctl', 'bootstatus', device.udid, '-b']);
  }

  for (const name of modules) {
    let output;
    if (platform === 'wasmWasi') {
      const entry = pathToFileURL(resolve(root,
        `build/artifacts/CompiledWebArtifact/${name}wasmWasiTestdebug/kotlin-output/${name}_test.mjs`));
      output = run(process.execPath, ['--input-type=module', '-e',
        `const tests = await import(${JSON.stringify(entry.href)}); tests.startUnitTests();`], true);
    } else {
      const executable = resolve(root, `build/tasks/_${name}_link${suffix}TestDebug/${name}_test.kexe`);
      output = run('xcrun', ['simctl', 'spawn', device.udid, executable, '--ktest_logger=TEAMCITY'], true);
    }
    const count = (output.match(/##teamcity\[testStarted /g) ?? []).length;
    // The WASI adapter can report failures without setting the process exit code.
    if (!count || /##teamcity\[testFailed /.test(output)) {
      process.stderr.write(output);
      throw new Error(`${name} (${platform}): test failure or no tests executed`);
    }
    console.log(`${name} (${platform}): ${count} tests passed`);
  }
} finally {
  if (bootedHere) run('xcrun', ['simctl', 'shutdown', device.udid]);
}
