import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { ToastrService } from 'ngx-toastr';

import { CityRegisterComponent } from './city-register.component';
import { CityService } from '../../../services/city.service';
import { IbgeService } from '../../../services/ibge.service';
import { City } from '../../../models/city';

describe('CityRegisterComponent', () => {
  let fixture: ComponentFixture<CityRegisterComponent>;
  let citySpy: jasmine.SpyObj<CityService>;
  let ibgeSpy: jasmine.SpyObj<IbgeService>;
  let toastrSpy: jasmine.SpyObj<ToastrService>;

  const curitiba: City = { id: 1, name: 'Curitiba', state: 'PR', ibgeCode: 4106902 };
  const londrinaAntiga: City = { id: 2, name: 'Londrina', state: 'PR' };

  const byId = (id: string) => fixture.nativeElement.querySelector(`[data-testid="${id}"]`) as HTMLElement | null;
  const click = (id: string) => { byId(id)!.click(); fixture.detectChanges(); };
  const escolher = (id: string, valor: string) => {
    const el = byId(id) as HTMLSelectElement;
    el.value = valor;
    el.dispatchEvent(new Event('change'));
    fixture.detectChanges();
  };
  const opcao = (select: string, texto: string) =>
    Array.from((byId(select) as HTMLSelectElement).options).find((o) => o.textContent!.includes(texto))!;

  beforeEach(async () => {
    citySpy = jasmine.createSpyObj('CityService', ['findAll', 'createFromIbge', 'delete']);
    citySpy.findAll.and.returnValue(of([curitiba, londrinaAntiga]));
    citySpy.createFromIbge.and.returnValue(of({ id: 3, name: 'Maringá', state: 'PR', ibgeCode: 4115200 }));
    citySpy.delete.and.returnValue(of(void 0));
    ibgeSpy = jasmine.createSpyObj('IbgeService', ['states', 'municipalities']);
    ibgeSpy.states.and.returnValue(of([{ code: 41, uf: 'PR', name: 'Paraná' }, { code: 42, uf: 'SC', name: 'Santa Catarina' }]));
    ibgeSpy.municipalities.and.returnValue(of([
      { code: 4106902, name: 'Curitiba' }, { code: 4113700, name: 'Londrina' }, { code: 4115200, name: 'Maringá' },
    ]));
    toastrSpy = jasmine.createSpyObj('ToastrService', ['success', 'error']);

    await TestBed.configureTestingModule({
      imports: [CityRegisterComponent],
      providers: [
        provideNoopAnimations(),
        { provide: CityService, useValue: citySpy },
        { provide: IbgeService, useValue: ibgeSpy },
        { provide: ToastrService, useValue: toastrSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CityRegisterComponent);
    fixture.detectChanges();
  });

  it('deveListarCidadesAtendidas', () => {
    expect(byId('city-1')?.textContent).toContain('Curitiba');
    expect(byId('city-1')?.textContent).toContain('PR');
    expect(fixture.nativeElement.textContent).toContain('2 cidades atendidas');

    const busca = byId('city-search') as HTMLInputElement;
    busca.value = 'londr';
    busca.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    expect(byId('city-1')).toBeNull();
    expect(byId('city-2')).not.toBeNull();
  });

  it('deveCarregarMunicipiosAoEscolherUf', () => {
    click('new-city');
    expect((byId('new-municipality') as HTMLSelectElement).disabled).toBeTrue();

    escolher('new-uf', 'PR');

    expect(ibgeSpy.municipalities).toHaveBeenCalledWith('PR');
    expect((byId('new-municipality') as HTMLSelectElement).disabled).toBeFalse();
    expect(opcao('new-municipality', 'Maringá')).toBeTruthy();
  });

  it('deveDesabilitarMunicipioJaAtendido', () => {
    click('new-city');
    escolher('new-uf', 'PR');

    expect(opcao('new-municipality', 'Curitiba').disabled).toBeTrue();
    expect(opcao('new-municipality', 'Curitiba').textContent).toContain('já atendida');
    // cidade antiga sem código casa por nome + UF
    expect(opcao('new-municipality', 'Londrina').disabled).toBeTrue();
    expect(opcao('new-municipality', 'Maringá').disabled).toBeFalse();
  });

  it('deveCadastrarPeloCodigoIbge', () => {
    click('new-city');
    expect((byId('save-city') as HTMLButtonElement).disabled).toBeTrue();
    escolher('new-uf', 'PR');
    escolher('new-municipality', '4115200');

    click('save-city');

    expect(citySpy.createFromIbge).toHaveBeenCalledWith(4115200);
    expect(toastrSpy.success).toHaveBeenCalledWith('Maringá - PR agora é atendida.');
    expect(byId('new-city-modal')).toBeNull();
    expect(citySpy.findAll).toHaveBeenCalledTimes(2);
  });

  it('deveConfirmarAntesDeExcluir', () => {
    click('delete-1');
    expect(citySpy.delete).not.toHaveBeenCalled();
    expect(byId('delete-modal')?.textContent).toContain('Deixar de atender Curitiba - PR?');

    click('confirm-delete');

    expect(citySpy.delete).toHaveBeenCalledWith(1);
    expect(toastrSpy.success).toHaveBeenCalledWith('Curitiba - PR deixou de ser atendida.');
  });

  it('deveMostrarMensagemQuandoCidadeEmUso', () => {
    citySpy.delete.and.returnValue(throwError(() => ({
      status: 409, error: { message: 'Curitiba - PR tem fornecedores ou endereços cadastrados e não pode ser excluída.' },
    })));

    click('delete-1');
    click('confirm-delete');

    expect(toastrSpy.error).toHaveBeenCalledWith(jasmine.stringContaining('tem fornecedores ou endereços cadastrados'));
    expect(byId('city-1')).not.toBeNull();
  });

  it('deveExibirEstadoVazio', () => {
    citySpy.findAll.and.returnValue(of([]));
    fixture.componentInstance.load();
    fixture.detectChanges();

    expect(byId('empty-state')?.textContent).toContain('Nenhuma cidade atendida');
  });
});
