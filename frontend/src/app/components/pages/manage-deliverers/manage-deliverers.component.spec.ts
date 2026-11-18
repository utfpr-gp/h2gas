import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { ToastrService } from 'ngx-toastr';

import { ManageDeliverersComponent } from './manage-deliverers.component';
import { DelivererService } from '../../../services/deliverer.service';
import { Deliverer } from '../../../models/deliverer';

describe('ManageDeliverersComponent', () => {
  let component: ManageDeliverersComponent;
  let fixture: ComponentFixture<ManageDeliverersComponent>;
  let delivererServiceSpy: jasmine.SpyObj<DelivererService>;
  let toastrSpy: jasmine.SpyObj<ToastrService>;

  const carlos: Deliverer = { id: 1, name: 'Carlos Silva', email: 'carlos@h2gas.local', phoneNumber: '41988887777', vehicleInfo: 'Moto - ABC-1234', active: true, busy: true, averageRating: 4.6, ratingCount: 1 };
  const ana: Deliverer = { id: 2, name: 'Ana Souza', email: 'ana@h2gas.local', phoneNumber: '41977776666', vehicleInfo: 'Carro', active: true, busy: false };

  const preencher = (campos: Record<string, string>) => component.form.patchValue(campos);

  beforeEach(async () => {
    delivererServiceSpy = jasmine.createSpyObj('DelivererService', ['listMine', 'create', 'resetPassword', 'setActive']);
    delivererServiceSpy.resetPassword.and.returnValue(of(void 0));
    delivererServiceSpy.listMine.and.returnValue(of([carlos, ana]));
    delivererServiceSpy.create.and.returnValue(of(ana));
    toastrSpy = jasmine.createSpyObj('ToastrService', ['success', 'warning', 'error']);

    await TestBed.configureTestingModule({
      imports: [ManageDeliverersComponent],
      providers: [
        { provide: DelivererService, useValue: delivererServiceSpy },
        { provide: ToastrService, useValue: toastrSpy },
        provideNoopAnimations(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ManageDeliverersComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // HU24
  it('deveMostrarANotaDoEntregador', () => {
    const linhas = Array.from(fixture.nativeElement.querySelectorAll('[data-testid=deliverer-row]')) as HTMLElement[];

    expect(linhas[0].querySelector('[data-testid=deliverer-rating]')?.textContent).toMatch(/4[.,]6/);
    expect(linhas[0].querySelector('[data-testid=deliverer-rating]')?.textContent).toContain('1 avaliação');
    expect(linhas[1].querySelector('[data-testid=deliverer-rating]')?.textContent).toContain('Sem avaliações');
  });

  it('deveListarEntregadoresComStatus', () => {
    const linhas = Array.from(fixture.nativeElement.querySelectorAll('[data-testid=deliverer-row]')) as HTMLElement[];

    expect(linhas.length).toBe(2);
    expect(linhas[0].textContent).toContain('Carlos Silva');
    expect(linhas[0].querySelector('[data-testid=deliverer-status]')?.textContent?.trim()).toBe('Em rota');
    expect(linhas[1].querySelector('[data-testid=deliverer-status]')?.textContent?.trim()).toBe('Disponível');
    // HU28 Cenário 5: telefone legível
    expect(linhas[0].textContent).toContain('(41) 98888-7777');
  });

  it('naoDeveCadastrarComDadosInvalidos', () => {
    preencher({ name: 'Bruno', email: 'nao-e-email', phoneNumber: '', password: '123' });

    component.save();

    expect(delivererServiceSpy.create).not.toHaveBeenCalled();
    expect(component.form.invalid).toBeTrue();
  });

  it('deveCadastrarEntregadorEAtualizarLista', () => {
    const dados = { name: 'Ana Souza', email: 'ana@h2gas.local', phoneNumber: '41977776666', password: 'senha123', vehicleInfo: 'Carro' };
    preencher(dados);

    component.save();

    expect(delivererServiceSpy.create).toHaveBeenCalledOnceWith(dados);
    expect(delivererServiceSpy.listMine).toHaveBeenCalledTimes(2);
    expect(toastrSpy.success).toHaveBeenCalled();
    expect(component.form.value.name).toBeFalsy();
  });

  it('deveMostrarMensagemDoBackendQuandoCadastroFalhar', () => {
    delivererServiceSpy.create.and.returnValue(throwError(() => ({ error: { error: 'Email já cadastrado.' } })));
    preencher({ name: 'Ana', email: 'ana@h2gas.local', phoneNumber: '41977776666', password: 'senha123' });

    component.save();

    expect(toastrSpy.error).toHaveBeenCalledWith(jasmine.stringMatching('Email já cadastrado.'));
    expect(component.saving).toBeFalse();
  });

  // ---- HU22: e-mail visível e senha redefinida pelo fornecedor ----
  const linha = (i: number) => (Array.from(fixture.nativeElement.querySelectorAll('[data-testid=deliverer-row]')) as HTMLElement[])[i];
  const abrirCard = (i: number) => {
    (linha(i).querySelector('[data-testid=deliverer-toggle]') as HTMLButtonElement).click();
    fixture.detectChanges();
  };
  const abrirRedefinicao = (i: number) => {
    abrirCard(i);
    (linha(i).querySelector('[data-testid=reset-password]') as HTMLButtonElement).click();
    fixture.detectChanges();
  };
  const digitar = (i: number, id: string, valor: string) => {
    const el = linha(i).querySelector(`[data-testid=${id}]`) as HTMLInputElement;
    el.value = valor;
    el.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  };
  const salvarSenha = (i: number) => linha(i).querySelector('[data-testid=save-password]') as HTMLButtonElement;

  it('deveMostrarOEmailAoTocarNoCard', () => {
    expect(linha(0).querySelector('[data-testid=deliverer-details]')).withContext('fechado de início').toBeNull();

    abrirCard(0);

    expect(linha(0).querySelector('[data-testid=deliverer-toggle]')?.getAttribute('aria-expanded')).toBe('true');
    expect(linha(0).querySelector('[data-testid=deliverer-email]')?.textContent).toContain('carlos@h2gas.local');
    expect(linha(0).querySelector('[data-testid=copy-email]')).not.toBeNull();
  });

  it('deveRedefinirASenha', () => {
    abrirRedefinicao(1);
    digitar(1, 'new-password', 'novaSenha9');
    digitar(1, 'confirm-password', 'novaSenha9');

    salvarSenha(1).click();
    fixture.detectChanges();

    expect(delivererServiceSpy.resetPassword).toHaveBeenCalledOnceWith(2, 'novaSenha9');
    expect(toastrSpy.success).toHaveBeenCalledWith('Senha de Ana Souza redefinida.');
    expect(linha(1).querySelector('[data-testid=new-password]')).toBeNull();
  });

  it('deveExigirSenhasIguaisEComSeisCaracteres', () => {
    abrirRedefinicao(0);
    digitar(0, 'new-password', '123');
    digitar(0, 'confirm-password', '123');
    expect(salvarSenha(0).disabled).toBeTrue();

    digitar(0, 'new-password', 'novaSenha9');
    digitar(0, 'confirm-password', 'outraSenha');
    expect(linha(0).querySelector('[data-testid=password-mismatch]')?.textContent).toContain('As senhas não conferem.');
    expect(salvarSenha(0).disabled).toBeTrue();
    expect(delivererServiceSpy.resetPassword).not.toHaveBeenCalled();
  });

  it('deveMostrarOErroDoBackendAoRedefinir', () => {
    delivererServiceSpy.resetPassword.and.returnValue(throwError(() => ({ status: 403, error: { error: 'Entregador não pertence à sua frota.' } })));
    abrirRedefinicao(0);
    digitar(0, 'new-password', 'novaSenha9');
    digitar(0, 'confirm-password', 'novaSenha9');

    salvarSenha(0).click();

    expect(toastrSpy.error).toHaveBeenCalledWith(jasmine.stringContaining('Entregador não pertence à sua frota.'));
  });

  it('deveAbrirUmCardPorVez', () => {
    abrirCard(0);
    abrirCard(1);

    expect(linha(0).querySelector('[data-testid=deliverer-details]')).toBeNull();
    expect(linha(1).querySelector('[data-testid=deliverer-details]')).not.toBeNull();

    abrirCard(1);
    expect(linha(1).querySelector('[data-testid=deliverer-details]')).withContext('tocar de novo fecha').toBeNull();
  });

  // ---- HU29: desativar e reativar ----
  const linhaDe = (nome: string) => (Array.from(fixture.nativeElement.querySelectorAll('[data-testid=deliverer-row]')) as HTMLElement[])
    .find(l => l.textContent?.includes(nome))!;
  const abrirPorNome = (nome: string) => {
    (linhaDe(nome).querySelector('[data-testid=deliverer-toggle]') as HTMLButtonElement).click();
    fixture.detectChanges();
  };

  it('deveDesativarComConfirmacao', () => {
    delivererServiceSpy.setActive.and.returnValue(of({ ...ana, active: false }));
    abrirPorNome('Ana Souza');

    (linhaDe('Ana Souza').querySelector('[data-testid=deactivate]') as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(delivererServiceSpy.setActive).not.toHaveBeenCalled();

    (linhaDe('Ana Souza').querySelector('[data-testid=confirm-deactivate]') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(delivererServiceSpy.setActive).toHaveBeenCalledWith(2, false);
    expect(linhaDe('Ana Souza').querySelector('[data-testid=deliverer-status]')?.textContent?.trim()).toBe('Desativado');
    expect(toastrSpy.success).toHaveBeenCalled();
  });

  it('deveReativarEntregadorDesativado', () => {
    delivererServiceSpy.listMine.and.returnValue(of([carlos, { ...ana, active: false }]));
    delivererServiceSpy.setActive.and.returnValue(of({ ...ana, active: true }));
    component.load();
    fixture.detectChanges();
    abrirPorNome('Ana Souza');

    (linhaDe('Ana Souza').querySelector('[data-testid=reactivate]') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(delivererServiceSpy.setActive).toHaveBeenCalledWith(2, true);
    expect(linhaDe('Ana Souza').querySelector('[data-testid=deliverer-status]')?.textContent?.trim()).toBe('Disponível');
  });

  it('deveMostrarDesativadosNoFim', () => {
    delivererServiceSpy.listMine.and.returnValue(of([{ ...ana, name: 'Ana Souza', active: false }, carlos]));
    component.load();
    fixture.detectChanges();

    const nomes = (Array.from(fixture.nativeElement.querySelectorAll('[data-testid=deliverer-row]')) as HTMLElement[])
      .map(l => l.textContent ?? '');
    expect(nomes[0]).toContain('Carlos Silva');
    expect(nomes[1]).toContain('Ana Souza');
  });
});
