import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { ToastrService } from 'ngx-toastr';

import { ProductItemComponent } from './product-item.component';
import { ProductItemService } from '../../../services/product-item.service';
import { UserService } from '../../../services/user.service';
import { ConfigurationService } from '../../../services/configuration.service';
import { ProductItem } from '../../../models/product-item';

describe('ProductItemComponent', () => {
  let component: ProductItemComponent;
  let fixture: ComponentFixture<ProductItemComponent>;

  const produto = (over: Partial<ProductItem>) =>
    ({ id: 1, brand: 'Ultragaz', category: 'Gás 13kg', type: 'Gás', stockQuantity: 10, ...over } as ProductItem);

  beforeEach(async () => {
    const productItemServiceSpy = jasmine.createSpyObj('ProductItemService', ['getProductsBySupllierId', 'delete']);
    productItemServiceSpy.getProductsBySupllierId.and.returnValue(of({
      data: [
        produto({ id: 1, priceDelivery: 120, pricePickup: 100, freeDelivery: false }),
        produto({ id: 2, brand: 'Liquigás', priceDelivery: 95, pricePickup: 95, freeDelivery: true }),
      ],
    } as any));
    const userServiceSpy = jasmine.createSpyObj('UserService', ['getUserLocalStorage']);
    userServiceSpy.getUserLocalStorage.and.returnValue({ id: 'f1', supplierId: 'f1' });
    const configurationServiceSpy = jasmine.createSpyObj('ConfigurationService', ['getSettings']);
    configurationServiceSpy.getSettings.and.returnValue({});

    await TestBed.configureTestingModule({
      imports: [ProductItemComponent],
      providers: [
        { provide: ProductItemService, useValue: productItemServiceSpy },
        { provide: UserService, useValue: userServiceSpy },
        { provide: ConfigurationService, useValue: configurationServiceSpy },
        { provide: ToastrService, useValue: jasmine.createSpyObj('ToastrService', ['success', 'error', 'warning']) },
        provideRouter([]),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ProductItemComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('deveExibirOsDoisPrecosEBadgeNaGestao', () => {
    const el = fixture.nativeElement as HTMLElement;
    const entrega = Array.from(el.querySelectorAll('[data-testid="manage-price-delivery"]')) as HTMLElement[];
    const retirada = Array.from(el.querySelectorAll('[data-testid="manage-price-pickup"]')) as HTMLElement[];
    expect(entrega.length).toBe(2);
    expect(entrega[0].textContent).toMatch(/120[.,]00/);
    expect(retirada[0].textContent).toMatch(/100[.,]00/);
    const selos = el.querySelectorAll('[data-testid="manage-free-delivery"]');
    expect(selos.length).toBe(1);
    expect(selos[0].textContent).toContain('Entrega Grátis');
  });
});
