import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { discoverModules, selectShard } from '../ci-model.mjs';

const modules = discoverModules();
const libraries = modules.filter(m => m.library).map(m => m.name);
const workflow = readFileSync(new URL('../../.github/workflows/ci.yml', import.meta.url), 'utf8');

test('documentation shards cover every production library exactly once', () => {
  for (const count of [1, 2, 3, libraries.length]) {
    const shards = Array.from({ length: count }, (_, i) => selectShard(libraries, `${i + 1}/${count}`));
    assert.ok(shards.every(shard => shard.length));
    assert.deepEqual(shards.flat().sort(), [...libraries].sort());
  }
  assert.throws(() => selectShard(libraries, '0/3'));
  assert.throws(() => selectShard(libraries, '4/3'));
  assert.throws(() => selectShard(libraries, '1/100'));
});

test('API matrix checks every supported library target exactly once', () => {
  const partitions = [...workflow.matchAll(/targets: '([^']+)'/g)].map(match => match[1].split(','));
  const targets = partitions.flat();
  const declared = [...new Set(modules.filter(m => m.library).flatMap(m => m.platforms))]
    .filter(target => !['android', 'watchosDeviceArm64'].includes(target)).sort();
  assert.deepEqual([...targets].sort(), declared);
  assert.equal(new Set(targets).size, targets.length);
});

test('test module discovery includes cross-module and fixture suites but excludes benchmark apps', () => {
  for (const platform of ['jvm', 'linuxX64', 'macosArm64', 'iosSimulatorArm64', 'js']) {
    const names = modules.filter(m => m.tests && m.platforms.includes(platform)).map(m => m.name);
    for (const required of ['kotmark', 'integration-test', 'test-fixtures', 'kotmark-ext-autolink']) assert.ok(names.includes(required));
    assert.ok(names.every(name => !name.startsWith('benchmark')));
  }
});

test('every library platform retains tests or explicit device compilation', () => {
  const platforms = [...new Set(modules.filter(m => m.library).flatMap(m => m.platforms))];
  const matrix = workflow.split('  jvm-quality:')[0];
  const selected = [...matrix.matchAll(/platforms: ([A-Za-z0-9 ]+)\n/g)].flatMap(match => match[1].split(' '));
  assert.deepEqual([...new Set([...selected, 'jvm', 'linuxArm64'])].sort(), platforms.sort());
});
