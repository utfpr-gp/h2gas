import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { ToastrService } from 'ngx-toastr';

import { AdminCategoriesComponent } from './admin-categories.component';
import { CategoryProductService } from '../../../services/category-product.service';
import { ProductTypeService } from '../../../services/product-type.service';
import { CategoryProduct } from '../../../models/category-product';
import { ProductType } from '../../../models/product-type';

// HU19: o admin mantém as categorias (P13, P45, Galão 20L…), globais, agrupadas por tipo
describe('AdminCategoriesComponent', () => {
  let fixture: ComponentFixture<AdminCategoriesComponent>;
  let categorySpy: jasmine.SpyObj<CategoryProductService>;
  let typeSpy: jasmine.SpyObj<ProductTypeService>;
  let toastrSpy: jasmine.SpyObj<ToastrService>;

  const tipos: ProductType[] = [{ id: 1, name: 'Gás', icon: 'local_fire_department' }, { id: 2, name: 'Água Mineral', icon: 'water_drop' }];
  const categorias: CategoryProduct[] = [
    { id: 10, name: 'P13', type: 'Gás', productTypeId: 1 },
    { id: 11, name: 'P45', type: 'Gás', productTypeId: 1 },
    { id: 12, name: '20L', type: 'Água Mineral', productTypeId: 2 },
  ];

  const byId = (id: string) => fixture.nativeElement.querySelector(`[data-testid="${id}"]`) as HTMLElement | null;
  const click = (id: string) => { byId(id)!.click(); fixture.detectChanges(); };
  const preencher = (id: string, valor: string) => {
    const el = byId(id) as HTMLInputElement;
    el.value = valor;
    el.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  };
  // as opções usam [ngValue] (valores internos do Angular): escolhe-se pelo nome do tipo, como o usuário faria
  const escolherTipo = (id: number) => {
    const el = byId('new-category-type') as HTMLSelectElement;
    const nome = tipos.find((t) => t.id === id)!.name;
    el.selectedIndex = Array.from(el.options).findIndex((o) => o.textContent?.trim() === nome);
    el.dispatchEvent(new Event('change'));
    fixture.detectChanges();
  };

  beforeEach(async () => {
    categorySpy = jasmine.createSpyObj('CategoryProductService', ['findAll', 'createCategory', 'updateCategory', 'delete']);
    categorySpy.findAll.and.returnValue(of(categorias));
    typeSpy = jasmine.createSpyObj('ProductTypeService', ['findAll']);
    typeSpy.findAll.and.returnValue(of(tipos));
    toastrSpy = jasmine.createSpyObj('ToastrService', ['success', 'error']);

    await TestBed.configureTestingModule({
      imports: [AdminCategoriesComponent],
      providers: [
        provideNoopAnimations(),
        { provide: CategoryProductService, useValue: categorySpy },
        { provide: ProductTypeService, useValue: typeSpy },
        { provide: ToastrService, useValue: toastrSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminCategoriesComponent);
    fixture.detectChanges();
  });

  it('deveListarAsCategoriasAgrupadasPorTipo', () => {
    expect(byId('group-1')?.textContent).toContain('Gás');
    expect(byId('group-1')?.textContent).toContain('P13');
    expect(byId('group-1')?.textContent).toContain('P45');
    expect(byId('group-2')?.textContent).toContain('20L');
    expect(byId('group-2')?.textContent).not.toContain('P13');
  });

  it('deveCriarCategoriaNoTipoEscolhido', () => {
    categorySpy.createCategory.and.returnValue(of({ id: 13, name: 'P5', type: 'Gás', productTypeId: 1 }));

    escolherTipo(1);
    preencher('new-category-name', 'P5');
    click('save-new-category');

    expect(categorySpy.createCategory).toHaveBeenCalledWith({ name: 'P5', productTypeId: 1 });
    expect(byId('category-13')?.textContent).toContain('P5');
    expect((byId('new-category-name') as HTMLInputElement).value).toBe('');
    expect(toastrSpy.success).toHaveBeenCalled();
  });

  it('naoDeveCriarSemNomeOuSemTipo', () => {
    click('save-new-category');
    escolherTipo(1);
    click('save-new-category');

    expect(categorySpy.createCategory).not.toHaveBeenCalled();
  });

  it('deveSugerirNomesPeloTipoEscolhido', () => {
    escolherTipo(1);
    expect(byId('suggestion-P13')).not.toBeNull();
    expect(byId('suggestion-20L')).toBeNull();

    click('suggestion-P13');
    expect((byId('new-category-name') as HTMLInputElement).value).toBe('P13');

    escolherTipo(2);
    expect(byId('suggestion-20L')).not.toBeNull();
    expect(byId('suggestion-P13')).toBeNull();
  });

  it('deveMostrarErroDeCategoriaRepetida', () => {
    categorySpy.createCategory.and.returnValue(throwError(() =>
      new HttpErrorResponse({ status: 400, error: { error: 'Categoria já cadastrada.' } })));

    escolherTipo(1);
    preencher('new-category-name', 'p13');
    click('save-new-category');

    expect(toastrSpy.error).toHaveBeenCalledWith(jasmine.stringContaining('Categoria já cadastrada.'));
    expect(fixture.nativeElement.querySelectorAll('li[data-testid^="category-"]').length).toBe(3);
  });

  it('deveRenomearCategoria', () => {
    categorySpy.updateCategory.and.returnValue(of({ id: 10, name: 'P13 Residencial', type: 'Gás', productTypeId: 1 }));

    click('rename-10');
    preencher('rename-input-10', 'P13 Residencial');
    click('save-rename-10');

    expect(categorySpy.updateCategory).toHaveBeenCalledWith(10, { name: 'P13 Residencial', productTypeId: 1 });
    expect(byId('category-10')?.textContent).toContain('P13 Residencial');
    expect(byId('rename-input-10')).toBeNull();
  });

  it('deveExcluirSoDepoisDeConfirmar', () => {
    categorySpy.delete.and.returnValue(of(undefined));

    click('delete-11');
    expect(categorySpy.delete).not.toHaveBeenCalled();
    click('confirm-delete-11');

    expect(categorySpy.delete).toHaveBeenCalledWith(11);
    expect(byId('category-11')).toBeNull();
  });

  it('deveCancelarAExclusao', () => {
    click('delete-11');
    click('cancel-delete-11');

    expect(byId('confirm-delete-11')).toBeNull();
    expect(categorySpy.delete).not.toHaveBeenCalled();
  });

  it('deveMostrarOMotivoQuandoACategoriaTemProdutos', () => {
    categorySpy.delete.and.returnValue(throwError(() =>
      new HttpErrorResponse({ status: 409, error: { message: 'P13 tem 3 produtos. Mova ou exclua os produtos antes de excluir a categoria.' } })));

    click('delete-10');
    click('confirm-delete-10');

    expect(toastrSpy.error).toHaveBeenCalledWith(jasmine.stringContaining('P13 tem 3 produtos'));
    expect(byId('category-10')).not.toBeNull();
  });
});
