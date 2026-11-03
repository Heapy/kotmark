import { spawnSync } from 'node:child_process';
import { appendFileSync, mkdirSync } from 'node:fs';
import { resolve } from 'node:path';
import { root } from './ci-model.mjs';

export function runToolchain(args, { env = process.env, label = args.join(' ') } = {}) {
  const started = Date.now();
  console.log(`\n${label}`);
  const windows = process.platform === 'win32';
  const result = spawnSync(windows ? 'cmd.exe' : './kotlin', windows ? ['/d', '/c', 'kotlin.bat', ...args] : args,
    { cwd: root, env, stdio: 'inherit' });
  const seconds = Math.round((Date.now() - started) / 100) / 10;
  const directory = resolve(root, 'build/reports/ci');
  mkdirSync(directory, { recursive: true });
  appendFileSync(resolve(directory, 'timings.jsonl'), JSON.stringify({ label, seconds, exitCode: result.status, signal: result.signal }) + '\n');
  if (process.env.GITHUB_STEP_SUMMARY) appendFileSync(process.env.GITHUB_STEP_SUMMARY,
    `- ${label}: **${seconds}s** (${result.status === 0 ? 'passed' : 'failed'})\n`);
  if (result.error) throw result.error;
  if (result.status !== 0) throw new Error(`${label} failed: ${result.status ?? result.signal}`);
}
