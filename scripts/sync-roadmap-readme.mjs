#!/usr/bin/env node
// Sincronizza il mirror della roadmap nel README dalla fonte di verità.
// Fonte: backend/src/main/resources/roadmap.md
// Target: blocco fra <!-- ROADMAP:START --> e <!-- ROADMAP:END --> in README.md
// I heading vengono declassati (#->###, ##->####) per non collidere con la struttura del README.
// Uso: node scripts/sync-roadmap-readme.mjs   (esce con codice 1 se il README era out-of-sync)

import { readFileSync, writeFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const ROADMAP = join(root, 'backend/src/main/resources/roadmap.md');
const README = join(root, 'README.md');
const START = '<!-- ROADMAP:START -->';
const END = '<!-- ROADMAP:END -->';

const mirror = readFileSync(ROADMAP, 'utf8')
  .trimEnd()
  .split('\n')
  .map((l) => l.replace(/^(#{1,6})\s/, (_, h) => '#'.repeat(Math.min(h.length + 2, 6)) + ' '))
  .join('\n');

const readme = readFileSync(README, 'utf8');
const s = readme.indexOf(START);
const e = readme.indexOf(END);
if (s === -1 || e === -1) {
  console.error(`Marker ${START}/${END} non trovati in README.md`);
  process.exit(2);
}

const next = readme.slice(0, s + START.length) + '\n' + mirror + '\n' + readme.slice(e);
if (next === readme) {
  console.log('README roadmap già in sync.');
  process.exit(0);
}
writeFileSync(README, next);
console.log('README roadmap aggiornato dalla fonte di verità.');
process.exit(1);
