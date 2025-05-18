import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DistribuitorCreateComponent } from './distribuitor-create.component';

describe('DistribuitorCreateComponent', () => {
  let component: DistribuitorCreateComponent;
  let fixture: ComponentFixture<DistribuitorCreateComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DistribuitorCreateComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DistribuitorCreateComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
