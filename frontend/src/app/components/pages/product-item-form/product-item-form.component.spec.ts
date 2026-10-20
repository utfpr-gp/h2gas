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
      priceDelivery: 120, pricePickup: 100, stockQuantity: 10, ...over,
    });

  const setup = async (id: string | null = null) => {
    productItemServiceSpy = jasmine.createSpyObj('ProductItemService', ['createWithFile', 'update', 'findById']);
    productItemServiceSpy.createWithFile.and.returnValue(of({} as any));
    productItemServiceSpy.update.and.returnValue(of({} as any));
    productItemServiceSpy.findById.and.returnValue(of({
      id: 5, brand: 'Liquigás', categoryProductId: 3, description: 'Edição', priceDelivery: 95, pricePickup: 80,
      stockQuantity: 4, imageUrl: 'http://img/x.jpg',
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

  it('deveExigirAmbosOsCamposDePrecoParaSubmeter', async () => {
    await setup();
    component.file = arquivo;

    preencher({ priceDelivery: null });
    expect(component.formProductItem.invalid).toBeTrue();
    component.saveItemProduct();

    preencher({ priceDelivery: 120, pricePickup: null });
    expect(component.formProductItem.invalid).toBeTrue();
    component.saveItemProduct();

    expect(productItemServiceSpy.createWithFile).not.toHaveBeenCalled();
    expect(toastrSpy.warning).toHaveBeenCalledTimes(2);
    expect(component.isLoading).toBeFalse();
  });

  it('naoDeveAceitarPrecoNegativo', async () => {
    await setup();

    preencher({ priceDelivery: -1 });
    expect(component.formProductItem.get('priceDelivery')!.hasError('min')).toBeTrue();
    preencher({ priceDelivery: 120, pricePickup: -5 });
    expect(component.formProductItem.get('pricePickup')!.hasError('min')).toBeTrue();
  });

  it('deveEnviarPriceDeliveryEPricePickupAoSalvar', async () => {
    await setup();
    component.file = arquivo;
    preencher();

    component.saveItemProduct();

    const enviado = productItemServiceSpy.createWithFile.calls.mostRecent().args[0] as any;
    expect(enviado.priceDelivery).toBe(120);
    expect(enviado.pricePickup).toBe(100);
    expect('price' in enviado).toBeFalse();
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

  it('deveAvisarEntregaGratisQuandoPrecosForemIguais', async () => {
    await setup();

    preencher({ priceDelivery: 100, pricePickup: 100 });
    fixture.detectChanges();

    expect(byId('free-delivery-hint')!.textContent).toContain('Entrega Grátis');
  });

  it('naoDeveAvisarQuandoPrecosDiferentes', async () => {
    await setup();

    preencher({ priceDelivery: 120, pricePickup: 100 });
    fixture.detectChanges();

    expect(byId('free-delivery-hint')).toBeNull();
  });

  it('naoDeveAvisarComPrecosVazios', async () => {
    await setup();

    preencher({ priceDelivery: null, pricePickup: null });
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

  it('deveCarregarOsDoisPrecosNaEdicao', async () => {
    await setup('5');

    expect(component.formProductItem.value.priceDelivery).toBe(95);
    expect(component.formProductItem.value.pricePickup).toBe(80);
    expect(component.formProductItem.value.categoryProductId).toBe(3);
  });

  it('deveSalvarAEdicaoSemExigirNovaImagem', async () => {
    await setup('5');
    preencher({ priceDelivery: 90, pricePickup: 90 });

    component.updateProduct();

    const enviado = productItemServiceSpy.update.calls.mostRecent().args[1] as any;
    expect(productItemServiceSpy.update.calls.mostRecent().args[0]).toBe(5);
    expect(enviado.priceDelivery).toBe(90);
    expect(enviado.pricePickup).toBe(90);
    expect('price' in enviado).toBeFalse();
    expect(navigateSpy).toHaveBeenCalledWith(['/product-items']);
  });
});
