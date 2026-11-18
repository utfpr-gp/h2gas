import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { ToastrService } from 'ngx-toastr';

import { OrderResumeComponent } from './order-resume.component';
import { CartService } from '../../../services/cart.service';
import { AddressService } from '../../../services/address.service';
import { UserService } from '../../../services/user.service';
import { OrderService } from '../../../services/order.service';
import { LocationContextService } from '../../../services/location-context.service';
import { Address } from '../../../models/address';
import { SupplierService } from '../../../services/supplier.service';
import { CartApiService } from '../../../services/cart-api.service';
import { CartPreview, CartPreviewItem } from '../../../models/cart';

describe('OrderResumeComponent', () => {
  let component: OrderResumeComponent;
  let fixture: ComponentFixture<OrderResumeComponent>;
  let orderServiceSpy: jasmine.SpyObj<OrderService>;
  let locationContextService: LocationContextService;
  let cartServiceSpy: jasmine.SpyObj<CartService>;
  let toastrSpy: jasmine.SpyObj<ToastrService>;
  let cart: any[];
  let routerSpy: jasmine.SpyObj<Router>;
  let supplierServiceSpy: jasmine.SpyObj<SupplierService>;
  let queryModo: string | null;
  let buyNowItem: any;
  let cartApiSpy: jasmine.SpyObj<CartApiService>;
  // HU20: o preview devolve os dados do produto; estoque e disponibilidade ajustáveis por teste
  let indisponiveis: Record<number, CartPreviewItem['unavailableReason']>;
  const catalogo: Record<number, { brand: string; category: string; type: string; delivery: number; pickup: number }> = {
    5: { brand: 'Liquigás', category: 'Gás 13kg', type: 'Gás', delivery: 110, pickup: 95 },
    9: { brand: 'Ultragaz', category: 'Gás 13kg', type: 'Gás', delivery: 125, pickup: 110 },
  };
  // HU17: com CEP em entrega, o fake cobra R$ 9,80; o CEP 99999-999 fica fora da área
  const previewFake = (lines: { productId: number; quantity: number }[], mode: string, zip?: string): CartPreview => {
    const items = lines.map((line) => {
      const p = catalogo[line.productId];
      const unitPrice = mode === 'PICKUP' ? p.pickup : p.delivery;
      const reason = indisponiveis[line.productId] ?? null;
      return { productId: line.productId, brand: p.brand, category: p.category, type: p.type, imageUrl: null,
        quantity: line.quantity, stockQuantity: 10, unitPrice, totalPrice: unitPrice * line.quantity,
        available: !reason, unavailableReason: reason };
    });
    const subtotal = items.filter((i) => i.available).reduce((sum, i) => sum + i.totalPrice, 0);
    const entrega = mode !== 'PICKUP' && !!zip;
    const deliversHere = entrega ? zip !== '99999-999' : null;
    const deliveryFee = mode === 'PICKUP' ? 0 : deliversHere ? 9.8 : null;
    return { supplierId: lines[0]?.productId === 9 ? 'sup-2' : 'sup-1', supplierName: 'Gás Centro', items, subtotal,
      deliveryFee, distanceKm: deliversHere ? 2.4 : null, deliversHere, total: subtotal + (deliveryFee ?? 0) };
  };

  const casa: Address = { id: 1, addressName: 'Casa', streetName: 'Rua Um', number: '10', cityId: 1, cityName: 'Pato Branco' };
  const trabalho: Address = { id: 2, addressName: 'Trabalho', streetName: 'Rua Dois', number: '20', cityId: 1, cityName: 'Pato Branco' };

  beforeEach(async () => {
    localStorage.clear();
    cart = [{ productId: 5, quantity: 1 }];
    indisponiveis = {};
    cartServiceSpy = jasmine.createSpyObj('CartService',
      ['getCart', 'setCart', 'clear', 'getCartSupplierId', 'getDeliveryMode', 'setDeliveryMode', 'updateQuantity', 'removeProduct',
        'getBuyNow', 'updateBuyNowQuantity', 'clearBuyNow']);
    cartServiceSpy.getCartSupplierId.and.callFake(() => (cart.length ? 'sup-1' : null));
    queryModo = null;
    buyNowItem = null;
    cartServiceSpy.getBuyNow.and.callFake(() => (buyNowItem ? { ...buyNowItem } : null));
    cartServiceSpy.updateBuyNowQuantity.and.callFake((q: number) => { buyNowItem = { ...buyNowItem, quantity: q }; });
    cartServiceSpy.clearBuyNow.and.callFake(() => { buyNowItem = null; });
    cartServiceSpy.getDeliveryMode.and.returnValue('DELIVERY');
    cartServiceSpy.getCart.and.callFake(() => cart.map((item) => ({ ...item })));
    cartServiceSpy.updateQuantity.and.callFake((id: number, quantity: number) => {
      cart = cart.map((item) => item.productId === id ? { ...item, quantity } : item);
    });
    cartServiceSpy.removeProduct.and.callFake((id: number) => {
      cart = cart.filter((item) => item.productId !== id);
    });
    cartApiSpy = jasmine.createSpyObj('CartApiService', ['preview']);
    cartApiSpy.preview.and.callFake((lines, mode, zip) => of(previewFake(lines, mode, zip)));
    toastrSpy = jasmine.createSpyObj('ToastrService', ['success', 'warning', 'error', 'info']);
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    supplierServiceSpy = jasmine.createSpyObj('SupplierService', ['findById']);
    supplierServiceSpy.findById.and.returnValue(of({
      id: 'sup-1', name: 'Gás Centro', addressStreetName: 'Rua da Loja', addressNumber: '50',
      addressNeighborhood: 'Centro', cityName: 'Curitiba', openingTime: '08:00', closingTime: '18:00',
    } as any));
    const addressServiceSpy = jasmine.createSpyObj('AddressService', ['findAllByUser']);
    addressServiceSpy.findAllByUser.and.returnValue(of([trabalho, casa]));
    const userServiceSpy = jasmine.createSpyObj('UserService', ['getUserLocalStorage']);
    userServiceSpy.getUserLocalStorage.and.returnValue({ id: 'user-1' });
    orderServiceSpy = jasmine.createSpyObj('OrderService', ['createOrder', 'previewOrder']);
    orderServiceSpy.createOrder.and.returnValue(of({} as any));

    await TestBed.configureTestingModule({
      imports: [OrderResumeComponent],
      providers: [
        { provide: CartService, useValue: cartServiceSpy },
        { provide: CartApiService, useValue: cartApiSpy },
        { provide: AddressService, useValue: addressServiceSpy },
        { provide: UserService, useValue: userServiceSpy },
        { provide: OrderService, useValue: orderServiceSpy },
        { provide: Router, useValue: routerSpy },
        { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: { get: (k: string) => (k === 'modo' ? queryModo : null) } } } },
        { provide: SupplierService, useValue: supplierServiceSpy },
        provideNoopAnimations(),
        { provide: ToastrService, useValue: toastrSpy },
      ],
    }).compileComponents();

    locationContextService = TestBed.inject(LocationContextService);
    locationContextService.selectAddress(casa);

    fixture = TestBed.createComponent(OrderResumeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  function irParaPagamento() {
    component.nextStep();
    component.nextStep();
  }

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('devePermitirSelecionarEnderecoNoCheckoutSemRedigitar', () => {
    expect(component.addresses.length).toBe(2);
    expect(component.selectedAddressId).toBe(1);

    component.selectAddress(trabalho);

    expect(component.selectedAddressId).toBe(2);
    expect(locationContextService.current()?.addressId).toBe(2);
  });

  it('deveEnviarAddressIdEscolhidoAoCriarPedido', () => {
    component.selectAddress(trabalho);
    component.formOrder.get('paymentMethod')?.setValue('PIX');
    irParaPagamento();

    component.createOrder();

    expect(orderServiceSpy.createOrder).toHaveBeenCalledWith(
      jasmine.objectContaining({ addressId: 2, supplierRef: 'sup-1' })
    );
  });

  it('deveChamarPreviewAoAlterarModalidade', () => {
    expect(cartApiSpy.preview).toHaveBeenCalledWith([{ productId: 5, quantity: 1 }], 'DELIVERY', undefined);

    component.setDeliveryMode('PICKUP');

    expect(cartApiSpy.preview).toHaveBeenCalledWith([{ productId: 5, quantity: 1 }], 'PICKUP', undefined);
    expect(component.preview?.subtotal).toBe(95);
    expect(orderServiceSpy.previewOrder).not.toHaveBeenCalled();
  });

  // ---- HU20: dados do backend e item indisponível ----
  const abrirDeNovo = () => {
    fixture = TestBed.createComponent(OrderResumeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  };

  it('deveExibirOsDadosDoPreview', () => {
    fixture.detectChanges();
    const item = fixture.nativeElement.querySelector('[data-testid="resume-item"]') as HTMLElement;

    expect(item.textContent).toContain('Liquigás');
    expect(item.textContent).toContain('Gás 13kg');
    expect(item.textContent).toMatch(/110[.,]00/);
  });

  it('deveBloquearOCheckoutComItemIndisponivel', () => {
    indisponiveis = { 5: 'OUT_OF_STOCK' };
    abrirDeNovo();
    const el = fixture.nativeElement as HTMLElement;

    expect(el.querySelector('[data-testid="resume-item-unavailable"]')?.textContent).toContain('Sem estoque suficiente');
    expect((el.querySelector('[data-testid="next-step"]') as HTMLButtonElement).disabled).toBeTrue();
    expect(el.querySelector('[data-testid="resume-blocked"]')).not.toBeNull();

    component.nextStep();
    expect(component.step).toBe(1);

    component.step = 3;
    component.formOrder.patchValue({ paymentMethod: 'PIX' });
    component.createOrder();
    expect(orderServiceSpy.createOrder).not.toHaveBeenCalled();
  });

  it('deveRemoverItemIndisponivel', () => {
    cart = [{ productId: 5, quantity: 1 }, { productId: 9, quantity: 1 }];
    indisponiveis = { 9: 'STORE_CLOSED' };
    abrirDeNovo();
    expect(component.hasUnavailable).toBeTrue();

    indisponiveis = {};
    const remover = fixture.nativeElement.querySelectorAll('[data-testid="resume-item-remove"]')[1] as HTMLButtonElement;
    expect(remover.textContent).toContain('Remover');
    remover.click();
    fixture.detectChanges();

    expect(cartServiceSpy.removeProduct).toHaveBeenCalledWith(9);
    expect(component.productsCarts.map((p) => p.productId)).toEqual([5]);
    expect(component.hasUnavailable).toBeFalse();
  });

  it('naoDeveMaisCalcularTotalLocalmente', () => {
    expect((component as any).totalPrice).toBeUndefined();
    expect((component as any).totalPerUnit).toBeUndefined();

    component.setDeliveryMode('PICKUP');
    fixture.detectChanges();

    const subtotal = fixture.nativeElement.querySelector('[data-testid="order-subtotal"]') as HTMLElement;
    expect(subtotal.textContent).toMatch(/95[.,]00/);
  });

  it('deveEnviarDeliveryModeAoCriarPedido', () => {
    component.setDeliveryMode('PICKUP');
    component.formOrder.get('paymentMethod')?.setValue('PIX');
    irParaPagamento();

    component.createOrder();

    expect(orderServiceSpy.createOrder).toHaveBeenCalledWith(
      jasmine.objectContaining({ deliveryMode: 'PICKUP' })
    );
  });

  it('deveIniciarNaEtapaDeItens', () => {
    expect(component.step).toBe(1);
    expect(fixture.nativeElement.querySelector('[data-testid="step-items"]')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('[data-testid="step-payment"]')).toBeNull();
  });

  it('deveRecalcularPreviewAoAlterarQuantidade', () => {
    component.increaseQuantity(component.productsCarts[0]);

    expect(cartServiceSpy.updateQuantity).toHaveBeenCalledWith(5, 2);
    expect(component.productsCarts[0].quantity).toBe(2);
    expect(cartApiSpy.preview).toHaveBeenCalledWith([{ productId: 5, quantity: 2 }], 'DELIVERY', undefined);

    component.decreaseQuantity(component.productsCarts[0]);

    expect(cartServiceSpy.updateQuantity).toHaveBeenCalledWith(5, 1);
  });

  it('deveRemoverItemERecalcularPreview', () => {
    component.removeItem(component.productsCarts[0]);

    expect(cartServiceSpy.removeProduct).toHaveBeenCalledWith(5);
    expect(component.productsCarts.length).toBe(0);
    expect(component.preview).toBeUndefined();
  });

  it('naoDeveAvancarComCarrinhoVazio', () => {
    component.removeItem(component.productsCarts[0]);

    component.nextStep();

    expect(component.step).toBe(1);
    expect(toastrSpy.warning).toHaveBeenCalled();
  });

  it('naoDeveAvancarDoEnderecoSemSelecionar', () => {
    component.nextStep();
    component.selectedAddressId = undefined;

    component.nextStep();

    expect(component.step).toBe(2);
    expect(toastrSpy.warning).toHaveBeenCalled();
  });

  it('deveVoltarParaEtapaAnterior', () => {
    irParaPagamento();
    expect(component.step).toBe(3);

    component.previousStep();

    expect(component.step).toBe(2);
  });

  it('deveConfirmarPedidoSoNaEtapaDeResumo', () => {
    component.formOrder.get('paymentMethod')?.setValue('PIX');

    component.createOrder();
    expect(orderServiceSpy.createOrder).not.toHaveBeenCalled();

    irParaPagamento();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[data-testid="step-payment"]')).toBeTruthy();

    component.createOrder();
    expect(orderServiceSpy.createOrder).toHaveBeenCalled();
  });

  it('deveExibirCarregandoEnquantoPreviewNaoChega', () => {
    const resposta = new Subject<any>();
    cartApiSpy.preview.and.returnValue(resposta);

    component.setDeliveryMode('PICKUP');
    fixture.detectChanges();

    expect(component.previewLoading).toBeTrue();
    expect(fixture.nativeElement.querySelector('[data-testid="subtotal-loading"]')).toBeTruthy();

    resposta.next(previewFake([{ productId: 5, quantity: 1 }], 'PICKUP'));
    fixture.detectChanges();

    expect(component.previewLoading).toBeFalse();
    expect(fixture.nativeElement.querySelector('[data-testid="subtotal-loading"]')).toBeNull();
  });

  it('deveVoltarParaEtapaConcluidaAoClicarNoIndicador', () => {
    irParaPagamento();

    component.goToStep(1);
    expect(component.step).toBe(1);

    component.goToStep(3);
    expect(component.step).toBe(1);
  });

  it('deveMostrarConfirmacaoAntesDeNavegar', fakeAsync(() => {
    component.formOrder.get('paymentMethod')?.setValue('PIX');
    irParaPagamento();

    component.createOrder();

    expect(component.orderPlaced).toBeTrue();
    expect(routerSpy.navigate).not.toHaveBeenCalled();

    tick(1500);

    expect(routerSpy.navigate).toHaveBeenCalledWith(['/meus-pedidos']);
  }));

  it('naoDeveExibirModalidadeNaEtapaDeItens', () => {
    expect(component.step).toBe(1);
    expect(fixture.nativeElement.querySelector('[data-testid="mode-pickup"]')).toBeNull();
  });

  it('deveExibirModalidadeNaEtapaDeEntrega', () => {
    component.nextStep();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="mode-delivery"]')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('[data-testid="mode-pickup"]')).toBeTruthy();
  });

  it('deveAvancarSemEnderecoQuandoRetirada', () => {
    component.nextStep();
    component.setDeliveryMode('PICKUP');
    component.selectedAddressId = undefined;

    component.nextStep();

    expect(component.step).toBe(3);
  });

  it('deveExibirEnderecoDaLojaNaRetirada', () => {
    component.nextStep();
    component.setDeliveryMode('PICKUP');
    fixture.detectChanges();

    expect(supplierServiceSpy.findById).toHaveBeenCalledWith('sup-1');
    const loja = fixture.nativeElement.querySelector('[data-testid="pickup-stores"]') as HTMLElement;
    expect(loja.textContent).toContain('Gás Centro');
    expect(loja.textContent).toContain('Rua da Loja, 50');
    expect(fixture.nativeElement.querySelector('[data-testid="customer-addresses"]')).toBeNull();
  });

  it('deveEnviarPedidoDeRetiradaSemAddressId', () => {
    component.formOrder.get('paymentMethod')?.setValue('PIX');
    component.nextStep();
    component.setDeliveryMode('PICKUP');
    component.nextStep();

    component.createOrder();

    expect(orderServiceSpy.createOrder).toHaveBeenCalledWith(
      jasmine.objectContaining({ deliveryMode: 'PICKUP', addressId: undefined })
    );
  });

  it('deveExibirMensagemDoBackendQuandoPedidoFalhar', () => {
    orderServiceSpy.createOrder.and.returnValue(throwError(() => ({
      error: { error: 'Estoque insuficiente para o produto: Botijão P5' },
    })));
    component.formOrder.get('paymentMethod')?.setValue('PIX');
    irParaPagamento();

    component.createOrder();

    expect(toastrSpy.error).toHaveBeenCalledWith(jasmine.stringMatching('Estoque insuficiente para o produto: Botijão P5'));
    expect(component.isLoading).toBeFalse();
    expect(component.orderPlaced).toBeFalse();
  });

  // ---- F7 §5.23: compra direta ----
  const abrirCompraDireta = (item: any = { productId: 9, quantity: 1, supplierId: 'sup-2' }) => {
    queryModo = 'compra-direta';
    buyNowItem = item;
    fixture = TestBed.createComponent(OrderResumeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  };
  const byId = (id: string) => fixture.nativeElement.querySelector(`[data-testid="${id}"]`) as HTMLElement | null;

  it('deveAbrirNaEtapaDeEntregaNaCompraDireta', () => {
    abrirCompraDireta();

    expect(component.step).toBe(2);
    expect(byId('step-address')).not.toBeNull();
    expect(byId('buy-now-summary')!.textContent).toContain('Ultragaz');
    expect(byId('buy-now-summary')!.textContent).toContain('Gás 13kg');
  });

  it('deveUsarSoOProdutoDaCompraDireta', () => {
    abrirCompraDireta();

    expect(component.productsCarts.map((p) => p.productId)).toEqual([9]);
    expect(cartApiSpy.preview.calls.mostRecent().args[0]).toEqual([{ productId: 9, quantity: 1 }]);
  });

  it('deveManterOCarrinhoAoConcluirACompraDireta', () => {
    abrirCompraDireta();
    component.selectedAddressId = 1;
    component.step = 3;
    component.formOrder.patchValue({ paymentMethod: 'PIX' });

    component.createOrder();

    expect(orderServiceSpy.createOrder).toHaveBeenCalledTimes(1);
    expect(orderServiceSpy.createOrder.calls.mostRecent().args[0]).toEqual(jasmine.objectContaining({
      supplierRef: 'sup-2', items: [{ productItemRef: 9, quantity: 1 }],
    }));
    expect(cartServiceSpy.clearBuyNow).toHaveBeenCalled();
    expect(cartServiceSpy.setCart).not.toHaveBeenCalled();
    expect(cartServiceSpy.clear).not.toHaveBeenCalled();
    expect(cart.length).toBe(1);
  });

  it('deveIgnorarItemAvulsoSemOParametroDeCompraDireta', () => {
    buyNowItem = { productId: 9, quantity: 1, supplierId: 'sup-2' };
    queryModo = null;
    fixture = TestBed.createComponent(OrderResumeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.step).toBe(1);
    expect(component.productsCarts.map((p) => p.productId)).toEqual([5]);
    expect(cartServiceSpy.clearBuyNow).toHaveBeenCalled();
  });

  it('deveVoltarParaOInicioSemItemAvulso', () => {
    abrirCompraDireta(null);

    expect(toastrSpy.warning).toHaveBeenCalledWith('Escolha um produto para comprar agora.');
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/inicio']);
  });

  it('deveAjustarAQuantidadeNoResumoDoItem', () => {
    abrirCompraDireta();
    cartApiSpy.preview.calls.reset();

    (byId('buy-now-increase') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(cartServiceSpy.updateBuyNowQuantity).toHaveBeenCalledWith(2);
    expect(cartServiceSpy.updateQuantity).not.toHaveBeenCalled();
    expect(cartApiSpy.preview.calls.mostRecent().args[0]).toEqual([{ productId: 9, quantity: 2 }]);
    expect(byId('buy-now-quantity')!.textContent).toContain('2');
  });

  // ---- HU17: frete por km no resumo ----
  const comCep = (cep: string) => ({ ...casa, zipCode: cep });
  const ultimoCep = () => cartApiSpy.preview.calls.mostRecent().args[2];

  it('deveMandarOCepDoEnderecoNoPreview', () => {
    component.selectAddress(comCep('80420-090'));

    expect(ultimoCep()).toBe('80420-090');

    component.setDeliveryMode('PICKUP');
    expect(ultimoCep()).toBeUndefined();
  });

  it('deveMostrarSubtotalFreteETotal', () => {
    component.selectAddress(comCep('80420-090'));
    component.formOrder.patchValue({ paymentMethod: 'PIX' });
    irParaPagamento();
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;

    expect(el.querySelector('[data-testid="summary-subtotal"]')?.textContent).toMatch(/110[.,]00/);
    expect(el.querySelector('[data-testid="summary-fee"]')?.textContent).toMatch(/9[.,]80/);
    expect(el.querySelector('[data-testid="summary-fee"]')?.textContent).toMatch(/2[.,]4 km/);
    expect(el.querySelector('[data-testid="summary-total"]')?.textContent).toMatch(/119[.,]80/);
    expect(el.querySelector('[data-testid="order-subtotal"]')?.textContent).toMatch(/119[.,]80/);
  });

  it('deveMostrarFreteZeroNaRetirada', () => {
    component.formOrder.patchValue({ paymentMethod: 'PIX' });
    component.nextStep();
    component.setDeliveryMode('PICKUP');
    component.nextStep();
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).querySelector('[data-testid="summary-fee"]')?.textContent)
      .toContain('Grátis (retirada)');
  });

  it('deveBloquearEnderecoForaDaArea', () => {
    component.nextStep();
    component.selectAddress(comCep('99999-999'));
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).querySelector('[data-testid="delivery-blocked"]')?.textContent)
      .toContain('Esta loja não entrega neste endereço');
    component.nextStep();
    expect(component.step).toBe(2);
    expect(toastrSpy.warning).toHaveBeenCalledWith(jasmine.stringMatching('não entrega neste endereço'));
  });
});
