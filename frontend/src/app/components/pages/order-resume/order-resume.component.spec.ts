import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { Router } from '@angular/router';
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

  const casa: Address = { id: 1, addressName: 'Casa', streetName: 'Rua Um', number: '10', cityId: 1, cityName: 'Pato Branco' };
  const trabalho: Address = { id: 2, addressName: 'Trabalho', streetName: 'Rua Dois', number: '20', cityId: 1, cityName: 'Pato Branco' };

  beforeEach(async () => {
    localStorage.clear();
    cart = [{ id: 5, supplierId: 'sup-1', priceDelivery: 110, pricePickup: 95, quantitySelect: 1 }];
    cartServiceSpy = jasmine.createSpyObj('CartService',
      ['getCart', 'setCart', 'getDeliveryMode', 'setDeliveryMode', 'updateQuantity', 'removeProduct']);
    cartServiceSpy.getDeliveryMode.and.returnValue('DELIVERY');
    cartServiceSpy.getCart.and.callFake(() => cart.map((item) => ({ ...item })));
    cartServiceSpy.updateQuantity.and.callFake((id: number, quantity: number) => {
      cart = cart.map((item) => item.id === id ? { ...item, quantitySelect: quantity } : item);
    });
    cartServiceSpy.removeProduct.and.callFake((id: number) => {
      cart = cart.filter((item) => item.id !== id);
    });
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
    orderServiceSpy.previewOrder.and.callFake((req) => of(
      req.deliveryMode === 'PICKUP'
        ? { items: [{ productItemRef: 5, quantity: 1, unitPrice: 95, totalPrice: 95 }], subtotal: 95 }
        : { items: [{ productItemRef: 5, quantity: 1, unitPrice: 110, totalPrice: 110 }], subtotal: 110 }
    ));

    await TestBed.configureTestingModule({
      imports: [OrderResumeComponent],
      providers: [
        { provide: CartService, useValue: cartServiceSpy },
        { provide: AddressService, useValue: addressServiceSpy },
        { provide: UserService, useValue: userServiceSpy },
        { provide: OrderService, useValue: orderServiceSpy },
        { provide: Router, useValue: routerSpy },
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
    expect(orderServiceSpy.previewOrder).toHaveBeenCalledWith(
      { items: [{ productItemRef: 5, quantity: 1 }], deliveryMode: 'DELIVERY' }
    );

    component.setDeliveryMode('PICKUP');

    expect(orderServiceSpy.previewOrder).toHaveBeenCalledWith(
      { items: [{ productItemRef: 5, quantity: 1 }], deliveryMode: 'PICKUP' }
    );
    expect(component.preview?.subtotal).toBe(95);
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
    component.increaseQuantity(component.productsCarts![0]);

    expect(cartServiceSpy.updateQuantity).toHaveBeenCalledWith(5, 2);
    expect(component.productsCarts![0].quantitySelect).toBe(2);
    expect(orderServiceSpy.previewOrder).toHaveBeenCalledWith(
      { items: [{ productItemRef: 5, quantity: 2 }], deliveryMode: 'DELIVERY' }
    );

    component.decreaseQuantity(component.productsCarts![0]);

    expect(cartServiceSpy.updateQuantity).toHaveBeenCalledWith(5, 1);
  });

  it('deveRemoverItemERecalcularPreview', () => {
    component.removeItem(component.productsCarts![0]);

    expect(cartServiceSpy.removeProduct).toHaveBeenCalledWith(5);
    expect(component.productsCarts!.length).toBe(0);
    expect(component.preview).toBeUndefined();
  });

  it('naoDeveAvancarComCarrinhoVazio', () => {
    component.removeItem(component.productsCarts![0]);

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
    orderServiceSpy.previewOrder.and.returnValue(resposta);

    component.setDeliveryMode('PICKUP');
    fixture.detectChanges();

    expect(component.previewLoading).toBeTrue();
    expect(fixture.nativeElement.querySelector('[data-testid="subtotal-loading"]')).toBeTruthy();

    resposta.next({ items: [{ productItemRef: 5, quantity: 1, unitPrice: 95, totalPrice: 95 }], subtotal: 95 });
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
});
