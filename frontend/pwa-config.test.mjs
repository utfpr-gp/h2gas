import { existsSync, readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import assert from 'node:assert/strict';
import { describe, it } from 'node:test';

const frontendRoot = dirname(fileURLToPath(import.meta.url));
const ICON_SIZES = [72, 96, 128, 144, 152, 192, 384, 512];

function readJson(relativePath) {
  const absolutePath = join(frontendRoot, relativePath);
  assert.ok(existsSync(absolutePath), `arquivo ausente: ${relativePath}`);
  return JSON.parse(readFileSync(absolutePath, 'utf8'));
}

function pngSize(relativePath) {
  const bytes = readFileSync(join(frontendRoot, relativePath));
  assert.equal(bytes.subarray(0, 8).toString('binary'), '\x89PNG\r\n\x1a\n');
  return {
    width: bytes.readUInt32BE(16),
    height: bytes.readUInt32BE(20),
  };
}

describe('F5 — ngsw-config.json', () => {
  it('deve cobrir /h2gas/api/** com estratégia freshness sem cache de negócio', () => {
    const config = readJson('ngsw-config.json');
    const apiGroup = (config.dataGroups || []).find((group) => group.name === 'api-freshness');

    assert.ok(apiGroup, 'dataGroups deve ter api-freshness');
    assert.deepEqual(apiGroup.urls, ['/h2gas/api/**']);
    assert.equal(apiGroup.cacheConfig.strategy, 'freshness');
    assert.equal(apiGroup.cacheConfig.maxSize, 0);
    assert.equal(apiGroup.cacheConfig.maxAge, '0u');
    assert.equal(apiGroup.cacheConfig.timeout, '5s');

    const assetFiles = (config.assetGroups || []).flatMap((group) => group.resources?.files || []);
    assert.equal(
      assetFiles.some((pattern) => String(pattern).includes('/api')),
      false,
      'rotas de API não podem estar em assetGroups (cache-first)',
    );
  });
});

describe('F5 — manifest.webmanifest', () => {
  it('deve usar nome H2Gás, cores dos tokens F2 e ícones reais', () => {
    const manifest = readJson('public/manifest.webmanifest');

    assert.equal(manifest.name, 'H2Gás');
    assert.equal(manifest.short_name, 'H2Gás');
    assert.equal(manifest.theme_color, '#0e2068');
    assert.equal(manifest.background_color, '#f7f8fc');
    assert.equal(manifest.display, 'standalone');
    assert.equal(manifest.scope, './');
    assert.equal(manifest.start_url, './');

    const expectedSrc = ICON_SIZES.map((size) => `icons/icon-${size}x${size}.png`);
    assert.deepEqual(
      (manifest.icons || []).map((icon) => icon.src),
      expectedSrc,
    );

    for (const icon of manifest.icons) {
      assert.equal(icon.type, 'image/png');
      assert.equal(icon.purpose, 'maskable any');
      const iconPath = join('public', icon.src);
      const { width, height } = pngSize(iconPath);
      const [expectedWidth, expectedHeight] = icon.sizes.split('x').map(Number);
      assert.equal(width, expectedWidth);
      assert.equal(height, expectedHeight);
    }
  });
});

describe('F5 — index.html e dependência', () => {
  it('deve referenciar o manifest e o theme-color no index.html', () => {
    const indexHtml = readFileSync(join(frontendRoot, 'src/index.html'), 'utf8');
    assert.match(indexHtml, /rel=["']manifest["']/);
    assert.match(indexHtml, /name=["']theme-color["']/);
    assert.match(indexHtml, /#0e2068/);
  });

  it('deve declarar @angular/service-worker no package.json', () => {
    const pkg = readJson('package.json');
    assert.ok(pkg.dependencies?.['@angular/service-worker']);
  });
});
