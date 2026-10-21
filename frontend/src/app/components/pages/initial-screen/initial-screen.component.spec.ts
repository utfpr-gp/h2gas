import { ComponentFixture, TestBed } from '@angular/core/testing';

import { InitialScreenComponent } from './initial-screen.component';
import { Router } from '@angular/router';

describe('InitialScreenComponent', () => {
  let component: InitialScreenComponent;
  let fixture: ComponentFixture<InitialScreenComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [InitialScreenComponent],
      providers: [{ provide: Router, useValue: jasmine.createSpyObj('Router', ['navigate']) }]
    })
    .compileComponents();

    fixture = TestBed.createComponent(InitialScreenComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // HU13 Cenário 1
  it('deveLevarOVisitanteParaAVitrine', () => {
    (fixture.nativeElement.querySelector('[data-testid="browse-as-visitor"]') as HTMLButtonElement).click();

    expect(TestBed.inject(Router).navigate).toHaveBeenCalledWith(['/inicio']);
  });
});
