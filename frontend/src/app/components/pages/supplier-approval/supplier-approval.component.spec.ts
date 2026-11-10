import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { ToastrService } from 'ngx-toastr';

import { SupplierApprovalComponent } from './supplier-approval.component';
import { SupplierService } from '../../../services/supplier.service';
import { Supplier } from '../../../models/supplier';
import { PaginatedResponse } from '../../../models/paginated-response';

describe('SupplierApprovalComponent', () => {
  let fixture: ComponentFixture<SupplierApprovalComponent>;
  let supplierSpy: jasmine.SpyObj<SupplierService>;
  let toastrSpy: jasmine.SpyObj<ToastrService>;

  const nova: Supplier = {
    id: 's-1', name: 'Gás Nova Era', cnpj: '11222333000144', email: 'nova@h2gas.local', phone: '46999990000',
    active: false, cityName: 'Pato Branco', addressNeighborhood: 'Centro', addressStreetName: 'Rua Itabira', addressNumber: '100',
  };
  const centro: Supplier = { id: 's-2', name: 'Gás Centro', email: 'centro@h2gas.local', active: true, cityName: 'Curitiba' };

  const pagina = (data: Supplier[], extra: Partial<PaginatedResponse<Supplier>> = {}): PaginatedResponse<Supplier> =>
    ({ data, page: 0, size: 10, totalElements: data.length, totalPages: 1, hasNext: false, hasPrevious: false, ...extra });

  const byId = (id: string) => fixture.nativeElement.querySelector(`[data-testid="${id}"]`) as HTMLElement | null;
  const click = (id: string) => { byId(id)!.click(); fixture.detectChanges(); };

  const setup = async (lista = () => of(pagina([{ ...nova }]))) => {
    supplierSpy = jasmine.createSpyObj('SupplierService', ['listForApproval', 'activeSuppliers']);
    supplierSpy.listForApproval.and.callFake(lista);
    supplierSpy.activeSuppliers.and.returnValue(of({ message: 'Status atualizado.' }));
    toastrSpy = jasmine.createSpyObj('ToastrService', ['success', 'error']);

    await TestBed.configureTestingModule({
      imports: [SupplierApprovalComponent],
      providers: [
        provideNoopAnimations(),
        { provide: SupplierService, useValue: supplierSpy },
        { provide: ToastrService, useValue: toastrSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SupplierApprovalComponent);
    fixture.detectChanges();
  };

  it('deveAbrirNoFiltroAguardandoAprovacao', async () => {
    await setup();

    expect(supplierSpy.listForApproval).toHaveBeenCalledWith(jasmine.objectContaining({ active: false, page: 0 }));
    expect(byId('chip-pending')?.getAttribute('aria-pressed')).toBe('true');
    const card = byId('supplier-s-1')!;
    expect(card.textContent).toContain('Gás Nova Era');
    expect(card.textContent).toContain('11222333000144');
    expect(card.textContent).toContain('Pato Branco');
    expect(card.textContent).toContain('(46) 99999-0000'); // HU28 Cenário 5
    expect(byId('status-s-1')?.textContent).toContain('Aguardando aprovação');
  });

  it('deveFiltrarPorStatus', async () => {
    await setup();

    click('chip-active');
    expect(supplierSpy.listForApproval).toHaveBeenCalledWith(jasmine.objectContaining({ active: true, page: 0 }));
    expect(byId('chip-active')?.getAttribute('aria-pressed')).toBe('true');

    click('chip-all');
    expect(supplierSpy.listForApproval.calls.mostRecent().args[0].active).toBeUndefined();
  });

  it('deveBuscarNoBackendComDebounce', fakeAsync(async () => {
    await setup();
    supplierSpy.listForApproval.calls.reset();
    const input = byId('supplier-search') as HTMLInputElement;

    input.value = 'nova';
    input.dispatchEvent(new Event('input'));
    tick(100);
    expect(supplierSpy.listForApproval).not.toHaveBeenCalled();
    tick(300);

    expect(supplierSpy.listForApproval).toHaveBeenCalledOnceWith(jasmine.objectContaining({ active: false, q: 'nova', page: 0 }));
  }));

  it('deveExibirEstadoVazio', async () => {
    await setup(() => of(pagina([])));

    expect(byId('empty-state')?.textContent).toContain('Nenhum fornecedor aguardando aprovação');
  });

  it('deveConfirmarAntesDeAprovar', async () => {
    await setup();

    click('toggle-s-1');

    expect(supplierSpy.activeSuppliers).not.toHaveBeenCalled();
    expect(byId('toggle-modal')?.textContent).toContain('Aprovar Gás Nova Era?');

    click('cancel-toggle');
    expect(byId('toggle-modal')).toBeNull();
    expect(supplierSpy.activeSuppliers).not.toHaveBeenCalled();
  });

  it('deveRecarregarAoAprovarComSucesso', async () => {
    await setup();
    supplierSpy.listForApproval.calls.reset();

    click('toggle-s-1');
    click('confirm-toggle');

    expect(supplierSpy.activeSuppliers).toHaveBeenCalledWith({ active: true }, 's-1');
    expect(toastrSpy.success).toHaveBeenCalledWith('Gás Nova Era foi aprovado e já aparece para os clientes.');
    expect(supplierSpy.listForApproval).toHaveBeenCalledWith(jasmine.objectContaining({ active: false, page: 0 }));
    expect(byId('toggle-modal')).toBeNull();
  });

  it('deveAvisarQueDesativarTiraDoCatalogo', async () => {
    await setup(() => of(pagina([{ ...centro }])));

    click('toggle-s-2');

    expect(byId('toggle-modal')?.textContent).toContain('Desativar Gás Centro?');
    expect(byId('toggle-modal')?.textContent).toContain('sai do catálogo dos clientes');
    click('confirm-toggle');
    expect(supplierSpy.activeSuppliers).toHaveBeenCalledWith({ active: false }, 's-2');
  });

  it('deveManterStatusQuandoAprovacaoFalhar', async () => {
    await setup();
    supplierSpy.activeSuppliers.and.returnValue(throwError(() => ({ status: 500, error: { message: 'Falha no banco.' } })));

    click('toggle-s-1');
    click('confirm-toggle');

    expect(toastrSpy.error).toHaveBeenCalledWith(jasmine.stringMatching(/^Não foi possível aprovar Gás Nova Era/));
    expect(byId('status-s-1')?.textContent).toContain('Aguardando aprovação');
    expect(byId('toggle-modal')).not.toBeNull();
  });

  it('devePaginar', async () => {
    await setup(() => of(pagina([{ ...nova }], { totalElements: 25, totalPages: 3 })));

    click('next-page');

    expect(supplierSpy.listForApproval).toHaveBeenCalledWith(jasmine.objectContaining({ page: 1 }));
    expect(fixture.nativeElement.textContent).toContain('25 fornecedores');
  });
});
