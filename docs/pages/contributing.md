---
title: Contributing
nav: Contributing
kicker: Project / working on kotmark
description: How to build, test, and contribute to kotmark.
lede: Bug reports, fixes, and new extensions are welcome. Run the checks relevant to your change before opening a pull request.
cards: []
---

## Set up

```shell label="Restore the locked build plugins"
git clone https://github.com/Heapy/kotmark.git
cd kotmark
./ktc-plugins sync
./ktc-plugins verify
```

The build uses the repository's `./kotlin` wrapper. Restore plugins from the lock file with `./ktc-plugins sync` and verify them with `./ktc-plugins verify`; do not edit restored files under `plugins/`. JVM artifacts and tests target Java 17.

## Checks

| Change | Verification |
| --- | --- |
| Parser or extension behavior | Run the module's JVM tests, add a focused regression, and run `integration-test` for interactions. |
| Public API | Run `node scripts/library-tools.mjs api-check <module>` before and after edits. |
| Documentation | Run the site-builder, `kotlinr docs/check.main.kts`, and the `examples` module tests. |
| Full JVM suite | Run `./kotlin test --platform jvm`. |

## Repository rules

- Keep CommonMark corpora complete and distinct; preserve upstream expected outputs.
- Keep extension dependencies scoped to their extension modules.
- Preserve third-party notices when adapting upstream code or fixtures.

## Target matrix {#targets}

Every declared target is built in CI; most also run the test suite.

| Targets | Verification |
| --- | --- |
| `jvm`, `android` | JVM suite and Android local unit tests |
| `linuxX64`, `linuxArm64` | Native tests |
| `mingwX64` | Native tests |
| `macosArm64`, `iosSimulatorArm64` | Native and iOS simulator tests |
| `js` | Node.js tests |
| `wasmJs` | Browser tests |
| `wasmWasi` | Node.js WASI tests |
| `tvosSimulatorArm64`, `watchosSimulatorArm64` | Simulator tests |
| `iosArm64`, `tvosArm64`, `watchosArm64`, `watchosDeviceArm64` | Compilation only; execution requires devices |

## Further evidence

- Generated [API reference](api/index.html) and [JVM line coverage](coverage/index.html) are rebuilt for each published revision.
- Property tests and fuzzing check parser invariants beyond the specification examples.
- API snapshots make changes to public declarations reviewable. The ABI is not frozen before the first release.
