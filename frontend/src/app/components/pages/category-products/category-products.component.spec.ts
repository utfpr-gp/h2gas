import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { ToastrService } from 'ngx-toastr';

import { CategoryProductsComponent } from './category-products.component';
import { CategoryProductService } from '../../../services/category-product.service';
import { ProductTypeService } from '../../../services/product-type.service';
import { SupplierCategory } from '../../../models/supplier-category';

describe('CategoryProductsComponent', () => {
  let fixture: ComponentFixture<CategoryProductsComponent>;
  let categorySpy: jasmine.SpyObj<CategoryProductService>;
  let toastrSpy: jasmine.SpyObj<ToastrService>;

  const categorias: SupplierCategory[] = [
    { id: 1, name: '20L', type: 'Água', productTypeId: 1, typeIcon: 'water_drop', productCount: 0 },
    { id: 2, name: 'P13', type: 'Gás', productTypeId: 2, typeIcon: 'local_fire_department', productCount: 3 },
    { id: 3, name: 'P45', type: 'Gás', productTypeId: 2, typeIcon: 'local_fire_department', productCount: 1 },
  ];

  const byId = (id: string) => fixture.nativeElement.querySelector(`[data-testid="${id}"]`) as HTMLElement | null;
  const click = (id: string) => { byId(id)!.click(); fixture.detectChanges(); };
  const preencher = (id: string, valor: string) => {
    const el = byId(id) as HTMLInputElement | HTMLSelectElement;
    el.value = valor;
    el.dispatchEvent(new Event(el instanceof HTMLSelectElement ? 'change' : 'input'));
    fixture.detectChanges();
  };

  const setup = async (lista = categorias) => {
    categorySpy = jasmine.createSpyObj('CategoryProductService', ['listMine', 'createCategory', 'updateCategory', 'delete']);
    categorySpy.listMine.and.returnValue(of(lista));
    categorySpy.createCategory.and.returnValue(of({ id: 9, name: 'P5' }));
    categorySpy.updateCategory.and.returnValue(of({ id: 2, name: 'P13 Plus' }));
    categorySpy.delete.and.returnValue(of(void 0));
    const typeSpy = jasmine.createSpyObj('ProductTypeService', ['findAll']);
    typeSpy.findAll.and.returnValue(of([{ id: 1, name: 'Água', icon: 'water_drop' }, { id: 2, name: 'Gás', icon: 'local_fire_department' }]));
    toastrSpy = jasmine.createSpyObj('ToastrService', ['success', 'error']);

    await TestBed.configureTestingModule({
      imports: [CategoryProductsComponent],
      providers: [
        provideNoopAnimations(),
        { provide: CategoryProductService, useValue: categorySpy },
        { provide: ProductTypeService, useValue: typeSpy },
        { provide: ToastrService, useValue: toastrSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CategoryProductsComponent);
    fixture.detectChanges();
  };

  it('deveAgruparCategoriasPorTipo', async () => {
    await setup();

    const grupos = Array.from(fixture.nativeElement.querySelectorAll('[data-testid^="group-"]')) as HTMLElement[];
    expect(grupos.map((g) => g.getAttribute('data-testid'))).toEqual(['group-1', 'group-2']);
    expect(byId('group-2')?.textContent).toContain('Gás');
    expect(byId('group-2')?.textContent).toContain('local_fire_department');
    expect(byId('category-2')?.textContent).toContain('3 produtos');
    expect(byId('category-3')?.textContent).toContain('1 produto');
    expect(byId('category-1')?.textContent).toContain('Sem produtos');
  });

  it('deveCriarCategoria', async () => {
    await setup();
    click('new-category');
    expect((byId('save-category') as HTMLButtonElement).disabled).toBeTrue();

    click('type-option-2');
    preencher('category-name', ' P5 ');
    click('save-category');

    expect(categorySpy.createCategory).toHaveBeenCalledWith({ name: 'P5', productTypeId: 2 });
    expect(toastrSpy.success).toHaveBeenCalledWith('Categoria P5 criada.');
    expect(byId('category-modal')).toBeNull();
    expect(categorySpy.listMine).toHaveBeenCalledTimes(2);
  });

  it('deveDesabilitarSalvarSemAlteracaoNaEdicao', async () => {
    await setup();
    click('edit-2');

    expect((byId('category-name') as HTMLInputElement).value).toBe('P13');
    expect((byId('save-category') as HTMLButtonElement).disabled).toBeTrue();

    preencher('category-name', 'P13 Plus');
    expect((byId('save-category') as HTMLButtonElement).disabled).toBeFalse();
    click('save-category');

    expect(categorySpy.updateCategory).toHaveBeenCalledWith(2, { name: 'P13 Plus', productTypeId: 2 });
  });

  it('deveMostrarMensagemDoBackendQuandoNomeRepetido', async () => {
    await setup();
    // criada em outra aba: a lista carregada não sabe dela, então só o backend barra
    categorySpy.createCategory.and.returnValue(throwError(() => ({ status: 409, error: { message: 'Você já tem a categoria P99 em Gás.' } })));
    click('new-category');
    click('type-option-2');
    preencher('category-name', 'P99');

    click('save-category');

    expect(toastrSpy.error).toHaveBeenCalledWith(jasmine.stringContaining('Você já tem a categoria P99 em Gás.'));
    expect(byId('category-modal')).not.toBeNull();
  });

  it('deveImpedirExcluirCategoriaComProdutos', async () => {
    await setup();

    click('delete-2');

    expect(byId('delete-modal')?.textContent).toContain('P13 tem 3 produtos');
    expect(byId('confirm-delete')).toBeNull();
    expect(categorySpy.delete).not.toHaveBeenCalled();
  });

  it('deveExcluirCategoriaVazia', async () => {
    await setup();

    click('delete-1');
    expect(byId('delete-modal')?.textContent).toContain('Excluir a categoria 20L?');
    click('confirm-delete');

    expect(categorySpy.delete).toHaveBeenCalledWith(1);
    expect(toastrSpy.success).toHaveBeenCalledWith('Categoria 20L excluída.');
  });

  it('deveExibirEstadoVazio', async () => {
    await setup([]);

    expect(byId('empty-state')?.textContent).toContain('Nenhuma categoria cadastrada');
  });

  describe('popup', () => {
    it('deveEscolherTipoPorCartaoComIcone', async () => {
      await setup();
      click('new-category');

      expect(byId('type-option-1')?.textContent).toContain('water_drop');
      expect(byId('type-option-2')?.textContent).toContain('Gás');
      expect(byId('type-option-2')?.getAttribute('aria-checked')).toBe('false');

      click('type-option-2');

      expect(byId('type-option-2')?.getAttribute('aria-checked')).toBe('true');
      expect(byId('type-option-1')?.getAttribute('aria-checked')).toBe('false');
    });

    it('deveSugerirNomesDoTipoSemOsQueJaExistem', async () => {
      await setup();
      click('new-category');
      expect(byId('name-suggestions')).toBeNull();

      click('type-option-2');
      const sugestoes = Array.from(byId('name-suggestions')!.querySelectorAll('button')).map((b) => b.textContent!.trim());
      expect(sugestoes).toEqual(['P2', 'P5', 'P8', 'P20']);

      click('suggestion-P20');
      expect((byId('category-name') as HTMLInputElement).value).toBe('P20');
      expect((byId('save-category') as HTMLButtonElement).disabled).toBeFalse();
    });

    it('deveEsconderSugestaoQueJaApareceNoNomeDeUmaCategoria', async () => {
      await setup([{ id: 7, name: 'Água 20L', type: 'Água', productTypeId: 1, typeIcon: 'water_drop', productCount: 2 }]);
      click('new-category');

      click('type-option-1');

      const sugestoes = Array.from(byId('name-suggestions')!.querySelectorAll('button')).map((b) => b.textContent!.trim());
      expect(sugestoes).toEqual(['5L', '10L']);
    });

    it('deveAvisarNomeRepetidoAntesDeEnviar', async () => {
      await setup();
      click('new-category');
      click('type-option-2');
      preencher('category-name', ' p13 ');

      expect(byId('name-duplicate')?.textContent).toContain('Você já tem P13 em Gás.');
      expect((byId('save-category') as HTMLButtonElement).disabled).toBeTrue();

      // a mesma categoria na edição não conta como repetida
      fixture.componentInstance.closeForm();
      fixture.detectChanges();
      click('edit-2');
      preencher('category-name', 'P13');
      expect(byId('name-duplicate')).toBeNull();
    });

    it('deveMostrarPreviaEContador', async () => {
      await setup();
      click('new-category');
      preencher('category-name', 'P5');
      expect(byId('category-preview')).toBeNull();

      click('type-option-2');

      expect(byId('category-preview')?.textContent).toContain('Gás · P5');
      expect(byId('name-counter')?.textContent).toContain('2/60');
    });

    it('deveFecharComEsc', async () => {
      await setup();
      click('new-category');

      byId('category-modal')!.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
      fixture.detectChanges();

      expect(byId('category-modal')).toBeNull();
    });
  });
});
