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

  const tipos: ProductType[] = [{ id: 1, name: 'Gás', icon: 'local_fire_department' }, { id: 2, name: 'Água Mineral', icon: 'water_drop' }];

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

    expect(typeSpy.create).toHaveBeenCalledWith({ name: 'Carvão', icon: 'category' });
    expect(byId('type-3')?.textContent).toContain('Carvão');
    expect((byId('new-type-name') as HTMLInputElement).value).toBe('');
    expect(toastrSpy.success).toHaveBeenCalled();
  });

  it('deveRenomearTipo', () => {
    typeSpy.update.and.returnValue(of({ id: 1, name: 'Gás GLP' }));

    click('rename-1');
    preencher('rename-input-1', 'Gás GLP');
    click('save-rename-1');

    expect(typeSpy.update).toHaveBeenCalledWith(1, { name: 'Gás GLP', icon: 'local_fire_department' });
    expect(byId('type-1')?.textContent).toContain('Gás GLP');
    expect(byId('rename-input-1')).toBeNull();
  });

  it('deveCriarTipoComIcone', () => {
    typeSpy.create.and.returnValue(of({ id: 3, name: 'Gelo', icon: 'ac_unit' }));

    preencher('new-type-name', 'Gelo');
    click('new-icon-ac_unit');
    expect(byId('new-icon-ac_unit')?.getAttribute('aria-pressed')).toBe('true');
    click('save-new-type');

    expect(typeSpy.create).toHaveBeenCalledWith({ name: 'Gelo', icon: 'ac_unit' });
    expect(byId('type-icon-3')?.textContent?.trim()).toBe('ac_unit');
    expect(byId('new-icon-category')?.getAttribute('aria-pressed')).toBe('true');
  });

  it('deveTrocarIconeAoEditar', () => {
    typeSpy.update.and.returnValue(of({ id: 1, name: 'Gás', icon: 'propane_tank' }));
    expect(byId('type-icon-1')?.textContent?.trim()).toBe('local_fire_department');

    click('rename-1');
    expect(byId('edit-icon-local_fire_department')?.getAttribute('aria-pressed')).toBe('true');
    click('edit-icon-propane_tank');
    click('save-rename-1');

    expect(typeSpy.update).toHaveBeenCalledWith(1, { name: 'Gás', icon: 'propane_tank' });
    expect(byId('type-icon-1')?.textContent?.trim()).toBe('propane_tank');
  });

  it('deveMostrarErroDeTipoRepetido', () => {
    typeSpy.create.and.returnValue(throwError(() =>
      new HttpErrorResponse({ status: 400, error: { message: 'Tipo já cadastrado.' } })));

    preencher('new-type-name', 'gás');
    click('save-new-type');

    expect(toastrSpy.error).toHaveBeenCalledWith(jasmine.stringContaining('Tipo já cadastrado.'));
    expect(fixture.nativeElement.querySelectorAll('li[data-testid^="type-"]').length).toBe(2);
  });
});
