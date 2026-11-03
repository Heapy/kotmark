---
title: Task list items
nav: Task list items
kicker: Extension 11 / kotmark-ext-task-list-items
description: Parse GitHub-style checkboxes in list items.
lede: Task states are represented in the AST and rendered as disabled checkbox inputs by the HTML renderer.
cards:
  - term: Artifact
    value: io.heapy.kotmark:kotmark-ext-task-list-items:0.1.0
  - term: Package
    value: io.heapy.kotmark.ext.task.list.items
---

## Usage

```kotlin caption="Enable task-list items" snippet=docs/examples/src/io/heapy/kotmark/docs/examples/extensions/TaskListItems.kt#task-list-items
```

## Example

```example example=task-list-items
- [x] Parse
- [ ] Render
```

## Output

The renderer marks the checkbox as checked or unchecked and disables it by default, keeping the checklist display-only.
