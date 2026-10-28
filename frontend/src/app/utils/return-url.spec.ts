import { Router } from '@angular/router';
import { isCheckoutReturn, navigateAfterLogin, navigateKeepingReturnUrl, safeReturnUrl } from './return-url';

// HU13: depois de entrar, o cliente volta para onde estava — só para caminho interno do app
describe('returnUrl', () => {
  it('deveAceitarCaminhoInterno', () => {
    expect(safeReturnUrl('/resumo-pedido?modo=compra-direta')).toBe('/resumo-pedido?modo=compra-direta');
    expect(safeReturnUrl('/meus-pedidos')).toBe('/meus-pedidos');
  });

  it('deveRecusarCaminhoExternoOuDeLogin', () => {
    for (const raw of [null, undefined, '', 'https://evil.com', '//evil.com', '/\\evil.com', 'javascript:alert(1)', '/login', '/login/senha', '/cadastro', '/cadastro/fornecedor', '/register']) {
      expect(safeReturnUrl(raw as any)).withContext(String(raw)).toBeNull();
    }
  });

  it('deveReconhecerORetornoAoCheckout', () => {
    expect(isCheckoutReturn('/resumo-pedido')).toBeTrue();
    expect(isCheckoutReturn('/meus-pedidos')).toBeFalse();
  });

  it('deveLevarOReturnUrlAdianteSoQuandoExiste', () => {
    const router = jasmine.createSpyObj<Router>('Router', ['navigate']);

    navigateKeepingReturnUrl(router, '/login', '/resumo-pedido');
    navigateKeepingReturnUrl(router, '/login', null);

    expect(router.navigate.calls.argsFor(0)).toEqual([['/login'], { queryParams: { returnUrl: '/resumo-pedido' } }]);
    expect(router.navigate.calls.argsFor(1)).toEqual([['/login']]);
  });

  it('deveVoltarAoReturnUrlOuIrParaOInicioDoPerfil', () => {
    const router = jasmine.createSpyObj<Router>('Router', ['navigate', 'navigateByUrl']);

    navigateAfterLogin(router, '/resumo-pedido?modo=compra-direta', 'CUSTOMER');
    navigateAfterLogin(router, 'https://evil.com', 'SUPPLIER');

    expect(router.navigateByUrl).toHaveBeenCalledOnceWith('/resumo-pedido?modo=compra-direta');
    expect(router.navigate).toHaveBeenCalledOnceWith(['/minha-loja/painel']);
  });
});
