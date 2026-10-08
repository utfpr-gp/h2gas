import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { of } from 'rxjs';

import { SupplierListComponent } from './supplier-list.component';
import { SupplierService } from '../../../services/supplier.service';
import { ProductItemService } from '../../../services/product-item.service';
import { LocationContextService } from '../../../services/location-context.service';

describe('SupplierListComponent', () => {
  let component: SupplierListComponent;
  let fixture: ComponentFixture<SupplierListComponent>;
  let supplierServiceSpy: jasmine.SpyObj<SupplierService>;
  let locationContextService: LocationContextService;

  const emptyPage = { data: [], page: 0, size: 10, totalElements: 0, totalPages: 0, hasNext: false, hasPrevious: false };

  beforeEach(async () => {
    localStorage.clear();
    supplierServiceSpy = jasmine.createSpyObj('SupplierService', ['getAllSupplierOpenAndActive', 'searchSuppliers']);
    supplierServiceSpy.getAllSupplierOpenAndActive.and.returnValue(of(emptyPage as any));

    await TestBed.configureTestingModule({
      imports: [SupplierListComponent],
      providers: [
        { provide: SupplierService, useValue: supplierServiceSpy },
        { provide: ProductItemService, useValue: jasmine.createSpyObj('ProductItemService', ['getProductsFromActiveSuppliers']) },
        { provide: Router, useValue: jasmine.createSpyObj('Router', ['navigate']) },
        { provide: ToastrService, useValue: jasmine.createSpyObj('ToastrService', ['success', 'error', 'warning']) },
      ]
    })
    .compileComponents();

    locationContextService = TestBed.inject(LocationContextService);
    fixture = TestBed.createComponent(SupplierListComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('deveEnviarCityIdDoContextoAoBuscarFornecedores', () => {
    locationContextService.setAddress({ cityId: 5, cityName: 'Pato Branco', label: 'Rua XV, 100 · Pato Branco' });

    fixture.detectChanges();

    expect(supplierServiceSpy.getAllSupplierOpenAndActive).toHaveBeenCalledWith(0, 10, 5);
  });
});
