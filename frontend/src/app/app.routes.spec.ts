import { Route } from '@angular/router';
import { routes } from './app.routes';

// F8: URLs das telas em português, por área; a URL antiga redireciona para a nova
const TABELA: [antiga: string, nova: string][] = [
  ['register', 'cadastro'],
  ['register-supplier', 'cadastro/fornecedor'],
  ['unauthorized', 'acesso-negado'],
  ['email-sent', 'email-enviado'],
  ['register-city', 'admin/cidades'],
  ['supplier-approval', 'admin/fornecedores'],
  ['distributor/new', 'admin/fornecedores/novo'],
  ['product-items', 'minha-loja/produtos'],
  ['product-items/new', 'minha-loja/produtos/novo'],
  ['product-items/edit/:id', 'minha-loja/produtos/:id/editar'],
  ['dashboard', 'minha-loja/painel'],
  ['dashboard-fornecedor', 'minha-loja/pedidos'],
  ['historico-pedidos', 'minha-loja/pedidos/historico'],
  ['entregadores', 'minha-loja/entregadores'],
  ['configuracao', 'minha-loja/configuracoes'],
  ['fornecedores/:id/produtos', 'lojas/:id'],
];

const rota = (path: string): Route | undefined => routes.find((r) => r.path === path);

describe('app.routes (F8)', () => {
  it('deveRedirecionarCadaUrlAntigaParaANova', () => {
    for (const [antiga, nova] of TABELA) {
      const legado = rota(antiga);
      expect(legado).withContext(antiga).toBeDefined();
      expect(legado?.redirectTo).withContext(antiga).toBe(nova);
      expect(legado?.pathMatch).withContext(antiga).toBe('full');
      expect(legado?.component ?? legado?.loadComponent).withContext(antiga).toBeUndefined();
    }
  });

  it('deveTerUmaTelaEmCadaUrlNova', () => {
    for (const [, nova] of TABELA) {
      const destino = rota(nova);
      expect(destino).withContext(nova).toBeDefined();
      expect(destino?.component ?? destino?.loadComponent).withContext(nova).toBeDefined();
    }
  });

  it('deveManterOsPapeisNasRotasNovas', () => {
    const papeis = (path: string) => rota(path)?.data?.['roles'];
    expect(papeis('admin/cidades')).toEqual(['ADMIN']);
    expect(papeis('admin/fornecedores')).toEqual(['ADMIN']);
    expect(papeis('admin/fornecedores/novo')).toEqual(['ADMIN', 'SUPPLIER']);
    for (const path of ['minha-loja/produtos', 'minha-loja/produtos/novo', 'minha-loja/produtos/:id/editar',
      'minha-loja/painel', 'minha-loja/pedidos/historico', 'minha-loja/entregadores', 'minha-loja/configuracoes']) {
      expect(papeis(path)).withContext(path).toEqual(['SUPPLIER']);
    }
    expect(papeis('minha-loja/pedidos')).toEqual(['ADMIN', 'SUPPLIER']);
    expect(papeis('lojas/:id')).toEqual(['ADMIN', 'SUPPLIER', 'CUSTOMER']);
    expect(rota('lojas/:id')?.data?.['allowVisitor']).toBeTrue();
  });

  it('naoDeveMudarOLinkDoEmailDeRedefinirSenha', () => {
    expect(rota('resetar-senha')?.component).toBeDefined();
  });
});
