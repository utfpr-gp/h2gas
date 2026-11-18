import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { ToastrModule, ToastrService } from 'ngx-toastr';

import { TOASTR_OPTIONS } from './toastr.config';

// F7 §5.10 (revisão 2026-10-18): um toast por vez, no topo central — o novo substitui o atual
describe('TOASTR_OPTIONS', () => {
  it('deveConfigurarToastrComOpcoesGlobais', () => {
    expect(TOASTR_OPTIONS).toEqual(jasmine.objectContaining({
      positionClass: 'toast-top-center',
      maxOpened: 1,
      autoDismiss: true,
      preventDuplicates: true,
      closeButton: true,
      progressBar: true,
    }));
  });

  it('deveSubstituirToastAtualPeloNovo', async () => {
    TestBed.configureTestingModule({
      imports: [ToastrModule.forRoot(TOASTR_OPTIONS)],
      providers: [provideNoopAnimations()],
    });
    const toastr = TestBed.inject(ToastrService);

    toastr.success('Produto adicionado ao carrinho.');
    toastr.error('Erro ao salvar: sem conexão com o servidor');
    // o anterior sai pela animação de saída do ngx-toastr antes de ser removido do DOM
    await new Promise((resolve) => setTimeout(resolve, 300));

    const abertos = Array.from(document.querySelectorAll('.ngx-toastr'));
    expect(abertos.length).toBe(1);
    expect(abertos[0].textContent).toContain('Erro ao salvar');

    toastr.clear();
  });
});
