import { registerLocaleData } from '@angular/common';
import localePt from '@angular/common/locales/pt';
import { LOCALE_ID } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { StoreReviewsDialogComponent } from './store-reviews-dialog.component';
import { SupplierService } from '../../services/supplier.service';
import { StoreReview } from '../../models/store-review';

registerLocaleData(localePt);

// F7 §5.27: janela com as avaliações da loja
describe('StoreReviewsDialogComponent', () => {
  let fixture: ComponentFixture<StoreReviewsDialogComponent>;
  let component: StoreReviewsDialogComponent;
  let supplierSpy: jasmine.SpyObj<SupplierService>;

  const pagina = (data: StoreReview[], page = 0, hasNext = false) =>
    ({ data, page, size: 10, totalElements: 12, totalPages: hasNext ? 2 : 1, hasNext, hasPrevious: page > 0 });
  const maria: StoreReview = { id: 1, customerFirstName: 'Maria', rating: 4, comment: 'Entrega rápida, recomendo.', reviewedAt: '2026-10-18T14:30:00' };
  const joao: StoreReview = { id: 2, customerFirstName: 'João', rating: 5, comment: null, reviewedAt: null };
  const byId = (id: string) => fixture.nativeElement.querySelector(`[data-testid="${id}"]`) as HTMLElement | null;
  const all = (id: string) => Array.from(fixture.nativeElement.querySelectorAll(`[data-testid="${id}"]`)) as HTMLElement[];

  const setup = () => {
    fixture = TestBed.createComponent(StoreReviewsDialogComponent);
    component = fixture.componentInstance;
    component.supplierId = 'loja-1';
    component.storeName = 'Gás Centro Curitiba';
    component.average = 4.2;
    component.count = 12;
    fixture.detectChanges();
  };

  beforeEach(async () => {
    supplierSpy = jasmine.createSpyObj('SupplierService', ['getReviews']);
    supplierSpy.getReviews.and.returnValue(of(pagina([maria, joao], 0, true)));
    await TestBed.configureTestingModule({
      imports: [StoreReviewsDialogComponent],
      providers: [provideNoopAnimations(), { provide: SupplierService, useValue: supplierSpy }, { provide: LOCALE_ID, useValue: 'pt-BR' }],
    }).compileComponents();
  });

  it('deveListarNomeNotaEComentario', () => {
    setup();

    expect(supplierSpy.getReviews).toHaveBeenCalledWith('loja-1', 0, 10);
    expect(fixture.nativeElement.querySelector('[role="dialog"]').textContent).toContain('Gás Centro Curitiba');
    const itens = all('review-item');
    expect(itens.length).toBe(2);
    expect(itens[0].textContent).toContain('Maria');
    expect(itens[0].textContent).toContain('18/10/2026');
    expect(itens[0].querySelector('[data-testid="review-stars"]')!.getAttribute('aria-label')).toBe('Nota 4 de 5');
    expect(itens[0].querySelector('[data-testid="review-comment"]')!.textContent).toContain('Entrega rápida, recomendo.');
  });

  it('naoDeveMostrarComentarioQuandoNaoHa', () => {
    setup();

    const joaoItem = all('review-item')[1];
    expect(joaoItem.textContent).toContain('João');
    expect(joaoItem.querySelector('[data-testid="review-comment"]')).toBeNull();
  });

  it('deveCarregarMaisAvaliacoes', () => {
    setup();
    supplierSpy.getReviews.and.returnValue(of(pagina([{ ...maria, id: 3, customerFirstName: 'Ana' }], 1, false)));

    (byId('load-more-reviews') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(supplierSpy.getReviews).toHaveBeenCalledWith('loja-1', 1, 10);
    expect(all('review-item').map((i) => i.querySelector('[data-testid="review-name"]')!.textContent!.trim()))
      .toEqual(['Maria', 'João', 'Ana']);
    expect(byId('load-more-reviews')).toBeNull();
  });

  it('deveFecharNoEsc', () => {
    setup();
    spyOn(component.closed, 'emit');

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));

    expect(component.closed.emit).toHaveBeenCalled();
  });

  it('deveMostrarErroComTentarDeNovo', () => {
    supplierSpy.getReviews.and.returnValue(throwError(() => ({ error: { message: 'Loja não encontrada' } })));
    setup();

    expect(byId('reviews-error')!.textContent).toContain('Loja não encontrada');
    supplierSpy.getReviews.and.returnValue(of(pagina([maria])));
    (byId('retry-reviews') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(byId('reviews-error')).toBeNull();
    expect(all('review-item').length).toBe(1);
  });
});
