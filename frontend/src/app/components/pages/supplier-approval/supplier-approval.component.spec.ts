import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SupplierApprovalComponent } from './supplier-approval.component';

describe('SupplierApprovalComponent', () => {
  let component: SupplierApprovalComponent;
  let fixture: ComponentFixture<SupplierApprovalComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SupplierApprovalComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SupplierApprovalComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
