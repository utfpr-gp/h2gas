import { existsSync, readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import assert from 'node:assert/strict';
import { describe, it } from 'node:test';

const distBrowser = join(dirname(fileURLToPath(import.meta.url)), 'dist/frontend/browser');

describe('F5 — artefatos do build de produção', () => {
  it('deve gerar ngsw.json, manifest.webmanifest e link no index.html', () => {
    assert.ok(existsSync(join(distBrowser, 'ngsw.json')), 'dist deve conter ngsw.json');
    assert.ok(
      existsSync(join(distBrowser, 'manifest.webmanifest')),
      'dist deve conter manifest.webmanifest',
    );

    const indexHtml = readFileSync(join(distBrowser, 'index.html'), 'utf8');
    assert.match(indexHtml, /rel=["']manifest["']/);
    assert.match(indexHtml, /ngsw-worker\.js|manifest\.webmanifest/);
  });
});
