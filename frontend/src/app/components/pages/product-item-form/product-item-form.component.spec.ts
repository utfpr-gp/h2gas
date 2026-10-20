import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { ToastrService } from 'ngx-toastr';

import { ProductItemFormComponent } from './product-item-form.component';
import { ProductItemService } from '../../../services/product-item.service';
import { ProductTypeService } from '../../../services/product-type.service';
import { CategoryProductService } from '../../../services/category-product.service';
import { UserService } from '../../../services/user.service';

describe('ProductItemFormComponent', () => {
  let component: ProductItemFormComponent;
  let fixture: ComponentFixture<ProductItemFormComponent>;
  let productItemServiceSpy: jasmine.SpyObj<ProductItemService>;
  let toastrSpy: jasmine.SpyObj<ToastrService>;
  let navigateSpy: jasmine.Spy;

  const el = () => fixture.nativeElement as HTMLElement;
  const byId = (id: string) => el().querySelector(`[data-testid=${id}]`) as HTMLElement | null;
  const arquivo = new File(['x'], 'botijao.jpg', { type: 'image/jpeg' });

  const preencher = (over: Record<string, unknown> = {}) =>
    component.formProductItem.patchValue({
      brand: 'Ultragaz', categoryProductId: 3, description: 'Botijão 13kg',
      price: 100, shippingFee: 20, stockQuantity: 10, ...over,
    });

  const setup = async (id: string | null = null) => {
    productItemServiceSpy = jasmine.createSpyObj('ProductItemService', ['createWithFile', 'update', 'findById']);
    productItemServiceSpy.createWithFile.and.returnValue(of({} as any));
    productItemServiceSpy.update.and.returnValue(of({} as any));
    productItemServiceSpy.findById.and.returnValue(of({
      id: 5, brand: 'Liquigás', categoryProductId: 3, description: 'Edição', priceDelivery: 95, pricePickup: 80,
      price: 80, shippingFee: 15, stockQuantity: 4, imageUrl: 'http://img/x.jpg', freeDelivery: false,
    } as any));
    const categoryServiceSpy = jasmine.createSpyObj('CategoryProductService', ['findBySupplierId']);
    categoryServiceSpy.findBySupplierId.and.returnValue(of({ data: [{ id: 3, name: 'Gás 13kg', type: 'Gás' }] } as any));
    const userServiceSpy = jasmine.createSpyObj('UserService', ['getUserLocalStorage']);
    userServiceSpy.getUserLocalStorage.and.returnValue({ id: 'f1', supplierId: 'f1' });
    toastrSpy = jasmine.createSpyObj('ToastrService', ['success', 'warning', 'error']);

    await TestBed.configureTestingModule({
      imports: [ProductItemFormComponent],
      providers: [
        { provide: ProductItemService, useValue: productItemServiceSpy },
        { provide: ProductTypeService, useValue: jasmine.createSpyObj('ProductTypeService', ['findAll']) },
        { provide: CategoryProductService, useValue: categoryServiceSpy },
        { provide: UserService, useValue: userServiceSpy },
        { provide: ToastrService, useValue: toastrSpy },
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap(id ? { id } : {}) } } },
      ],
    }).compileComponents();

    navigateSpy = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    fixture = TestBed.createComponent(ProductItemFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  };

  afterEach(() => fixture.destroy());

  it('should create', async () => {
    await setup();
    expect(component).toBeTruthy();
  });

  it('deveExigirPrecoEFreteParaSubmeter', async () => {
    await setup();
    component.file = arquivo;

    preencher({ price: null });
    expect(component.formProductItem.invalid).toBeTrue();
    component.saveItemProduct();

    preencher({ price: 100, shippingFee: null });
    expect(component.formProductItem.invalid).toBeTrue();
    component.saveItemProduct();

    expect(productItemServiceSpy.createWithFile).not.toHaveBeenCalled();
    expect(toastrSpy.warning).toHaveBeenCalledTimes(2);
    expect(component.isLoading).toBeFalse();
  });

  it('naoDeveAceitarValorNegativo', async () => {
    await setup();

    preencher({ price: -1 });
    expect(component.formProductItem.get('price')!.hasError('min')).toBeTrue();
    preencher({ price: 100, shippingFee: -5 });
    expect(component.formProductItem.get('shippingFee')!.hasError('min')).toBeTrue();
  });

  it('deveEnviarPriceEShippingFeeAoSalvar', async () => {
    await setup();
    component.file = arquivo;
    preencher();

    component.saveItemProduct();

    const enviado = productItemServiceSpy.createWithFile.calls.mostRecent().args[0] as any;
    expect(enviado.price).toBe(100);
    expect(enviado.shippingFee).toBe(20);
    expect('priceDelivery' in enviado).toBeFalse();
    expect('pricePickup' in enviado).toBeFalse();
    expect(productItemServiceSpy.createWithFile.calls.mostRecent().args[1]).toBe(arquivo);
    expect(navigateSpy).toHaveBeenCalledWith(['/product-items']);
  });

  it('deveExigirImagemNoCadastro', async () => {
    await setup();
    preencher();

    component.saveItemProduct();

    expect(productItemServiceSpy.createWithFile).not.toHaveBeenCalled();
    expect(toastrSpy.warning).toHaveBeenCalledWith(jasmine.stringMatching('imagem'));
    expect(component.isLoading).toBeFalse();
  });

  it('deveMostrarPreviaDeRetiradaEEntrega', async () => {
    await setup();

    preencher({ price: 100, shippingFee: 20 });
    fixture.detectChanges();

    const previa = byId('price-preview')!.textContent!;
    expect(previa).toContain('Retirada');
    expect(previa).toMatch(/100[.,]00/);
    expect(previa).toContain('Entrega');
    expect(previa).toMatch(/120[.,]00/);
  });

  it('naoDeveMostrarPreviaSemOsDoisValores', async () => {
    await setup();

    preencher({ price: 100, shippingFee: null });
    fixture.detectChanges();

    expect(byId('price-preview')).toBeNull();
  });

  it('deveAvisarEntregaGratisQuandoOFreteForZero', async () => {
    await setup();

    preencher({ price: 100, shippingFee: 0 });
    fixture.detectChanges();

    expect(byId('free-delivery-hint')!.textContent).toContain('Entrega Grátis');
  });

  it('naoDeveAvisarQuandoHouverFrete', async () => {
    await setup();

    preencher({ price: 100, shippingFee: 20 });
    fixture.detectChanges();
    expect(byId('free-delivery-hint')).toBeNull();

    preencher({ price: 100, shippingFee: null });
    fixture.detectChanges();
    expect(byId('free-delivery-hint')).toBeNull();
  });

  it('deveAceitarImagemSoltaNaAreaDeUpload', async () => {
    await setup();
    const area = byId('image-drop')!;
    const dataTransfer = new DataTransfer();
    dataTransfer.items.add(arquivo);

    area.dispatchEvent(new DragEvent('drop', { dataTransfer, bubbles: true, cancelable: true }));

    expect(component.file).toBe(arquivo);
  });

  it('deveIgnorarArquivoQueNaoEImagem', async () => {
    await setup();
    const dataTransfer = new DataTransfer();
    dataTransfer.items.add(new File(['x'], 'nota.pdf', { type: 'application/pdf' }));

    byId('image-drop')!.dispatchEvent(new DragEvent('drop', { dataTransfer, bubbles: true, cancelable: true }));

    expect(component.file).toBeUndefined();
    expect(toastrSpy.warning).toHaveBeenCalledWith(jasmine.stringMatching('imagem'));
  });

  it('deveVoltarParaListaAoCancelar', async () => {
    await setup();

    (byId('cancel-product') as HTMLButtonElement).click();

    expect(navigateSpy).toHaveBeenCalledWith(['/product-items']);
  });

  it('deveCarregarPrecoEFreteNaEdicao', async () => {
    await setup('5');

    expect(component.formProductItem.value.price).toBe(80);
    expect(component.formProductItem.value.shippingFee).toBe(15);
    expect(component.formProductItem.value.categoryProductId).toBe(3);
  });

  it('deveSalvarAEdicaoSemExigirNovaImagem', async () => {
    await setup('5');
    preencher({ price: 90, shippingFee: 0 });

    component.updateProduct();

    const enviado = productItemServiceSpy.update.calls.mostRecent().args[1] as any;
    expect(productItemServiceSpy.update.calls.mostRecent().args[0]).toBe(5);
    expect(enviado.price).toBe(90);
    expect(enviado.shippingFee).toBe(0);
    expect('priceDelivery' in enviado).toBeFalse();
    expect('pricePickup' in enviado).toBeFalse();
    expect(navigateSpy).toHaveBeenCalledWith(['/product-items']);
  });
});
