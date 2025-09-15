import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DistribuitorFormComponent } from './distribuitor-form.component';

describe('DistribuitorFormComponent', () => {
  let component: DistribuitorFormComponent;
  let fixture: ComponentFixture<DistribuitorFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DistribuitorFormComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DistribuitorFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
