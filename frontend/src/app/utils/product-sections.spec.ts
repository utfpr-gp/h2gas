import { groupProductsByTypeAndCategory } from './product-sections';

const tipos = [{ name: 'Gás' }, { name: 'Água' }];

describe('groupProductsByTypeAndCategory', () => {
  it('junta categorias do mesmo tipo e separa tipos na ordem dos chips', () => {
    const secoes = groupProductsByTypeAndCategory([
      { id: 1, type: 'Água', category: 'Água 20L' },
      { id: 2, type: 'Gás', category: 'Gás 45kg' },
      { id: 3, type: 'Gás', category: 'Gás 13kg' },
      { id: 4, type: 'Gás', category: 'Gás 13kg' },
    ], tipos);

    expect(secoes.map((s) => s.type)).toEqual(['Gás', 'Água']);
    expect(secoes[0].categories.map((c) => c.name)).toEqual(['Gás 13kg', 'Gás 45kg']);
    expect(secoes[0].categories[0].products.map((p) => p.id)).toEqual([3, 4]);
  });

  it('naoInventaSecaoVaziaQuandoOTipoJaEstaFiltrado', () => {
    const secoes = groupProductsByTypeAndCategory([
      { id: 2, type: 'Gás', category: 'Gás 45kg' },
      { id: 3, type: 'Gás', category: 'Gás 13kg' },
    ], tipos);

    expect(secoes.map((s) => s.type)).toEqual(['Gás']);
  });

  it('mandaItemSemTipoOuCategoriaParaOutros', () => {
    const secoes = groupProductsByTypeAndCategory([
      { id: 1, type: 'Gás', category: '' },
      { id: 2, type: '', category: 'Solta' },
    ], tipos);

    expect(secoes[0].categories.map((c) => c.name)).toContain('Outros');
    expect(secoes.map((s) => s.type)).toContain('Outros');
  });
});
