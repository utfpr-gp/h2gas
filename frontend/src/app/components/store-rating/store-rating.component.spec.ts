import { registerLocaleData } from '@angular/common';
import localePt from '@angular/common/locales/pt';
import { LOCALE_ID } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { StoreRatingComponent } from './store-rating.component';

registerLocaleData(localePt);

// F7 §5.25/§5.26: nota da loja, usada no card do catálogo e no cabeçalho da loja
describe('StoreRatingComponent', () => {
  let component: StoreRatingComponent;
  let fixture: ComponentFixture<StoreRatingComponent>;
  const rating = () => fixture.nativeElement.querySelector('[data-testid="supplier-rating"]') as HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StoreRatingComponent],
      providers: [{ provide: LOCALE_ID, useValue: 'pt-BR' }]
    }).compileComponents();

    fixture = TestBed.createComponent(StoreRatingComponent);
    component = fixture.componentInstance;
  });

  it('deveMostrarNotaEContagemDaLoja', () => {
    component.average = 4.3;
    component.count = 14;
    fixture.detectChanges();

    expect(rating().textContent).toContain('4,3');
    expect(rating().textContent).toContain('(14)');
    expect(rating().textContent).not.toContain('Sem avaliações');
    expect(rating().getAttribute('aria-label')).toBe('Nota 4,3 de 5, 14 avaliações');
  });

  it('deveMostrarSingularComUmaAvaliacao', () => {
    component.average = 5;
    component.count = 1;
    fixture.detectChanges();

    expect(rating().textContent).toContain('5,0');
    expect(rating().getAttribute('aria-label')).toBe('Nota 5,0 de 5, 1 avaliação');
  });

  it('deveMostrarSemAvaliacoesQuandoNaoHa', () => {
    component.average = null;
    component.count = 0;
    fixture.detectChanges();

    expect(rating().textContent).toContain('Sem avaliações ainda');
    expect(rating().getAttribute('aria-label')).toBeNull();
  });
});
