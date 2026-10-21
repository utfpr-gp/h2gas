import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PaginationComponent } from './pagination.component';

describe('PaginationComponent', () => {
  let component: PaginationComponent;
  let fixture: ComponentFixture<PaginationComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PaginationComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PaginationComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('deveMarcarAPaginaAtualSemFundoEscuro', () => {
    component.currentPage = 1;
    component.totalPages = 3;
    fixture.detectChanges();

    const atual = fixture.nativeElement.querySelector('[aria-current="page"]') as HTMLElement;
    expect(atual.textContent!.trim()).toBe('2');
    expect(atual.classList).not.toContain('bg-primary');
    expect(atual.classList).toContain('border-primary');
  });
});
