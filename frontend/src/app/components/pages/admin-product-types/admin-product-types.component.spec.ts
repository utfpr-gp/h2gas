import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { ToastrService } from 'ngx-toastr';

import { AdminProductTypesComponent } from './admin-product-types.component';
import { ProductTypeService } from '../../../services/product-type.service';
import { ProductType } from '../../../models/product-type';

describe('AdminProductTypesComponent', () => {
  let fixture: ComponentFixture<AdminProductTypesComponent>;
  let typeSpy: jasmine.SpyObj<ProductTypeService>;
  let toastrSpy: jasmine.SpyObj<ToastrService>;

  const tipos: ProductType[] = [{ id: 1, name: 'Gás' }, { id: 2, name: 'Água Mineral' }];

  const byId = (id: string) => fixture.nativeElement.querySelector(`[data-testid="${id}"]`) as HTMLElement | null;
  const click = (id: string) => { byId(id)!.click(); fixture.detectChanges(); };
  const preencher = (id: string, valor: string) => {
    const el = byId(id) as HTMLInputElement;
    el.value = valor;
    el.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  };

  beforeEach(async () => {
    typeSpy = jasmine.createSpyObj('ProductTypeService', ['findAll', 'create', 'update']);
    typeSpy.findAll.and.returnValue(of(tipos));
    toastrSpy = jasmine.createSpyObj('ToastrService', ['success', 'error']);

    await TestBed.configureTestingModule({
      imports: [AdminProductTypesComponent],
      providers: [
        provideNoopAnimations(),
        { provide: ProductTypeService, useValue: typeSpy },
        { provide: ToastrService, useValue: toastrSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminProductTypesComponent);
    fixture.detectChanges();
  });

  it('deveListarOsTipos', () => {
    expect(byId('type-1')?.textContent).toContain('Gás');
    expect(byId('type-2')?.textContent).toContain('Água Mineral');
  });

  it('deveCriarTipo', () => {
    typeSpy.create.and.returnValue(of({ id: 3, name: 'Carvão' }));

    preencher('new-type-name', 'Carvão');
    click('save-new-type');

    expect(typeSpy.create).toHaveBeenCalledWith({ name: 'Carvão' });
    expect(byId('type-3')?.textContent).toContain('Carvão');
    expect((byId('new-type-name') as HTMLInputElement).value).toBe('');
    expect(toastrSpy.success).toHaveBeenCalled();
  });

  it('deveRenomearTipo', () => {
    typeSpy.update.and.returnValue(of({ id: 1, name: 'Gás GLP' }));

    click('rename-1');
    preencher('rename-input-1', 'Gás GLP');
    click('save-rename-1');

    expect(typeSpy.update).toHaveBeenCalledWith(1, { name: 'Gás GLP' });
    expect(byId('type-1')?.textContent).toContain('Gás GLP');
    expect(byId('rename-input-1')).toBeNull();
  });

  it('deveMostrarErroDeTipoRepetido', () => {
    typeSpy.create.and.returnValue(throwError(() =>
      new HttpErrorResponse({ status: 400, error: { message: 'Tipo já cadastrado.' } })));

    preencher('new-type-name', 'gás');
    click('save-new-type');

    expect(toastrSpy.error).toHaveBeenCalledWith(jasmine.stringContaining('Tipo já cadastrado.'));
    expect(fixture.nativeElement.querySelectorAll('[data-testid^="type-"]').length).toBe(2);
  });
});
