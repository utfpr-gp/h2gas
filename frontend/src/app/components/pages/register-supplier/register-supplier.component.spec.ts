import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { provideNgxMask } from 'ngx-mask';

import { RegisterSupplierComponent } from './register-supplier.component';

describe('RegisterSupplierComponent', () => {
  let component: RegisterSupplierComponent;
  let fixture: ComponentFixture<RegisterSupplierComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RegisterSupplierComponent],
      // F11: dependências que o spec gerado pelo CLI nunca registrou
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        provideNoopAnimations(),
        provideNgxMask(),
        { provide: ToastrService, useValue: jasmine.createSpyObj('ToastrService', ['success', 'error', 'warning', 'info']) },
      ],
    })
    .compileComponents();

    fixture = TestBed.createComponent(RegisterSupplierComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
