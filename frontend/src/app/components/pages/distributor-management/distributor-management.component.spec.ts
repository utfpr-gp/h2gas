import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { ToastrService } from 'ngx-toastr';

import { DistributorManagementComponent } from './distributor-management.component';

describe('DistributorManagementComponent', () => {
  let component: DistributorManagementComponent;
  let fixture: ComponentFixture<DistributorManagementComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DistributorManagementComponent],
      // F11: dependências que o spec gerado pelo CLI nunca registrou
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        provideNoopAnimations(),
        { provide: ToastrService, useValue: jasmine.createSpyObj('ToastrService', ['success', 'error', 'warning', 'info']) },
      ],
    })
    .compileComponents();

    fixture = TestBed.createComponent(DistributorManagementComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
