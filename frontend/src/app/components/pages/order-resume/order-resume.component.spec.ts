import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of } from 'rxjs';
import { ToastrService } from 'ngx-toastr';

import { OrderResumeComponent } from './order-resume.component';
import { CartService } from '../../../services/cart.service';
import { AddressService } from '../../../services/address.service';
import { UserService } from '../../../services/user.service';
import { OrderService } from '../../../services/order.service';
import { LocationContextService } from '../../../services/location-context.service';
import { Address } from '../../../models/address';

describe('OrderResumeComponent', () => {
  let component: OrderResumeComponent;
  let fixture: ComponentFixture<OrderResumeComponent>;
  let orderServiceSpy: jasmine.SpyObj<OrderService>;
  let locationContextService: LocationContextService;

  const casa: Address = { id: 1, addressName: 'Casa', streetName: 'Rua Um', number: '10', cityId: 1, cityName: 'Pato Branco' };
  const trabalho: Address = { id: 2, addressName: 'Trabalho', streetName: 'Rua Dois', number: '20', cityId: 1, cityName: 'Pato Branco' };

  beforeEach(async () => {
    localStorage.clear();
    const cartServiceSpy = jasmine.createSpyObj('CartService', ['getCart', 'setCart']);
    cartServiceSpy.getCart.and.returnValue([
      { id: 5, supplierId: 'sup-1', price: 100, quantitySelect: 1 },
    ]);
    const addressServiceSpy = jasmine.createSpyObj('AddressService', ['findAllByUser']);
    addressServiceSpy.findAllByUser.and.returnValue(of([trabalho, casa]));
    const userServiceSpy = jasmine.createSpyObj('UserService', ['getUserLocalStorage']);
    userServiceSpy.getUserLocalStorage.and.returnValue({ id: 'user-1' });
    orderServiceSpy = jasmine.createSpyObj('OrderService', ['createOrder']);
    orderServiceSpy.createOrder.and.returnValue(of({} as any));

    await TestBed.configureTestingModule({
      imports: [OrderResumeComponent],
      providers: [
        { provide: CartService, useValue: cartServiceSpy },
        { provide: AddressService, useValue: addressServiceSpy },
        { provide: UserService, useValue: userServiceSpy },
        { provide: OrderService, useValue: orderServiceSpy },
        { provide: Router, useValue: jasmine.createSpyObj('Router', ['navigate']) },
        { provide: ToastrService, useValue: jasmine.createSpyObj('ToastrService', ['success', 'warning', 'error']) },
      ],
    }).compileComponents();

    locationContextService = TestBed.inject(LocationContextService);
    locationContextService.selectAddress(casa);

    fixture = TestBed.createComponent(OrderResumeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

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

    component.createOrder();

    expect(orderServiceSpy.createOrder).toHaveBeenCalledWith(
      jasmine.objectContaining({ addressId: 2, supplierRef: 'sup-1' })
    );
  });
});
