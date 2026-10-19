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

  const carlos: Deliverer = { id: 1, name: 'Carlos Silva', email: 'carlos@h2gas.local', phoneNumber: '41988887777', vehicleInfo: 'Moto - ABC-1234', active: true, busy: true };
  const ana: Deliverer = { id: 2, name: 'Ana Souza', email: 'ana@h2gas.local', phoneNumber: '41977776666', vehicleInfo: 'Carro', active: true, busy: false };

  const preencher = (campos: Record<string, string>) => component.form.patchValue(campos);

  beforeEach(async () => {
    delivererServiceSpy = jasmine.createSpyObj('DelivererService', ['listMine', 'create']);
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

  it('deveListarEntregadoresComStatus', () => {
    const linhas = Array.from(fixture.nativeElement.querySelectorAll('[data-testid=deliverer-row]')) as HTMLElement[];

    expect(linhas.length).toBe(2);
    expect(linhas[0].textContent).toContain('Carlos Silva');
    expect(linhas[0].querySelector('[data-testid=deliverer-status]')?.textContent?.trim()).toBe('Em rota');
    expect(linhas[1].querySelector('[data-testid=deliverer-status]')?.textContent?.trim()).toBe('Disponível');
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
});
