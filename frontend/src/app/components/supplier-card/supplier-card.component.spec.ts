import { registerLocaleData } from '@angular/common';
import localePt from '@angular/common/locales/pt';
import { LOCALE_ID } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SupplierCardComponent } from './supplier-card.component';
import { Supplier } from '../../models/supplier';

registerLocaleData(localePt);

describe('SupplierCardComponent', () => {
  let component: SupplierCardComponent;
  let fixture: ComponentFixture<SupplierCardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SupplierCardComponent],
      providers: [{ provide: LOCALE_ID, useValue: 'pt-BR' }]
    }).compileComponents();

    fixture = TestBed.createComponent(SupplierCardComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    component.supplier = { id: '7', name: 'Depósito Central' } as Supplier;
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('deveEmitirIdDoFornecedorAoClicarEmVerProdutos', () => {
    component.supplier = { id: '7', name: 'Depósito Central' } as Supplier;
    fixture.detectChanges();
    spyOn(component.viewProducts, 'emit');

    (fixture.nativeElement.querySelector('[data-testid="view-products"]') as HTMLButtonElement).click();

    expect(component.viewProducts.emit).toHaveBeenCalledWith('7');
  });

  it('deveMostrarLogoDaLojaOuIcone', () => {
    const el = fixture.nativeElement as HTMLElement;
    component.supplier = { id: '7', name: 'Depósito Central', logoUrl: 'https://cdn/logo.png' } as Supplier;
    fixture.detectChanges();

    const img = el.querySelector('[data-testid="supplier-logo"]') as HTMLImageElement;
    expect(img.src).toBe('https://cdn/logo.png');
    expect(img.alt).toBe('Logo de Depósito Central');
    expect(el.querySelector('[data-testid="supplier-icon"]')).toBeNull();

    // imagem quebrada volta para o ícone
    img.dispatchEvent(new Event('error'));
    fixture.detectChanges();
    expect(el.querySelector('[data-testid="supplier-logo"]')).toBeNull();
    expect(el.querySelector('[data-testid="supplier-icon"]')?.textContent).toContain('storefront');
  });

  it('deveMostrarIconeSemLogo', () => {
    component.supplier = { id: '8', name: 'Sem Logo' } as Supplier;
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="supplier-logo"]')).toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="supplier-icon"]')).not.toBeNull();
  });

  it('deveManterTextosLegiveisEAlvoDeToque', () => {
    component.supplier = { id: '7', name: 'Depósito Central' } as Supplier;
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;

    // cinza 400 não passa de 2,6:1 no branco; o aviso de avaliações usa um tom que passa de 4,5:1
    expect(el.querySelector('.text-gray-400')).toBeNull();
    expect(el.querySelector('[data-testid="view-products"]')!.classList).toContain('min-h-11');
  });

  // ---- F7 §5.25: a formatação da nota é testada em store-rating; aqui, só que o card a mostra ----
  const rating = () => fixture.nativeElement.querySelector('[data-testid="supplier-rating"]') as HTMLElement;

  it('deveMostrarNotaEContagemDaLoja', () => {
    component.supplier = { id: '7', name: 'Gás Centro', averageRating: 4.3, reviewCount: 14 } as Supplier;
    fixture.detectChanges();

    expect(rating().textContent).toContain('4,3');
    expect(rating().textContent).toContain('(14)');
    expect(rating().textContent).not.toContain('Sem avaliações');
    // F7 §5.27: no card, a nota é o botão que abre as avaliações
    expect(rating().getAttribute('aria-label')).toBe('Ver as 14 avaliações da loja, nota 4,3 de 5');
  });

  // ---- HU15 ----
  const selos = () => Array.from(
    (fixture.nativeElement as HTMLElement).querySelectorAll('[data-testid="supplier-type"]')) as HTMLElement[];

  it('deveMostrarOsSelosDeTipo', () => {
    component.supplier = {
      id: '7', name: 'Gás Centro', productTypes: [
        { id: 1, name: 'Água', icon: 'water_drop' },
        { id: 2, name: 'Gás', icon: 'local_fire_department' }
      ]
    } as Supplier;
    fixture.detectChanges();

    expect(selos().length).toBe(2);
    expect(selos()[0].textContent).toContain('Água');
    expect(selos()[1].textContent).toContain('Gás');
  });

  it('deveUsarOIconeDoTipo', () => {
    component.supplier = {
      id: '7', name: 'Gás Centro', productTypes: [{ id: 2, name: 'Gás', icon: 'local_fire_department' }]
    } as Supplier;
    fixture.detectChanges();

    expect(selos()[0].querySelector('.material-icons')?.textContent).toContain('local_fire_department');
    // o nome é texto de verdade, não só ícone: leitor de tela precisa ler o tipo
    expect(selos()[0].textContent).toContain('Gás');
  });

  it('naoDeveMostrarSeloQuandoALojaNaoTemTipo', () => {
    component.supplier = { id: '7', name: 'Loja Sem Catálogo', productTypes: [] } as unknown as Supplier;
    fixture.detectChanges();
    expect(selos().length).toBe(0);

    component.supplier = { id: '7', name: 'Loja Antiga' } as Supplier;
    fixture.detectChanges();
    expect(selos().length).toBe(0);
  });

  // ---- HU16: "abertas primeiro" traz lojas fechadas para a vitrine ----
  it('deveMarcarLojaFechadaComSelo', () => {
    component.supplier = { id: '7', name: 'Gás Centro', open: false, openingTime: '08:00' } as Supplier;
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[data-testid="supplier-closed"]')?.textContent).toContain('Fechada · abre às 08:00');

    component.supplier = { id: '7', name: 'Gás Centro', open: true } as Supplier;
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[data-testid="supplier-closed"]')).toBeNull();
  });

  it('deveAcinzentarALojaFechada', () => {
    const card = () => fixture.nativeElement.querySelector('[data-testid="view-products"]') as HTMLElement;
    component.supplier = { id: '7', name: 'Gás Centro', open: false } as Supplier;
    fixture.detectChanges();
    expect(card().classList).toContain('grayscale');
    expect(card().classList).toContain('opacity-60');

    component.supplier = { id: '7', name: 'Gás Centro', open: true } as Supplier;
    fixture.detectChanges();
    expect(card().classList).not.toContain('grayscale');
  });

  // ---- F7 §5.27 ----
  it('naoDeveAbrirProdutosAoClicarNasEstrelas', () => {
    component.supplier = { id: '7', name: 'Gás Centro', averageRating: 4.2, reviewCount: 14 } as Supplier;
    fixture.detectChanges();
    spyOn(component.viewProducts, 'emit');
    spyOn(component.viewReviews, 'emit');

    (fixture.nativeElement.querySelector('button[data-testid="supplier-rating"]') as HTMLButtonElement).click();

    expect(component.viewReviews.emit).toHaveBeenCalledWith(jasmine.objectContaining({ id: '7' }));
    expect(component.viewProducts.emit).not.toHaveBeenCalled();
  });

  // ---- HU17: distância e frete calculados no backend ----
  const linhaEntrega = () => (fixture.nativeElement.querySelector('[data-testid="supplier-delivery"]') as HTMLElement | null);

  it('deveMostrarDistanciaEFrete', () => {
    component.supplier = { id: '7', name: 'Gás Centro', offersDelivery: true, offersPickup: true,
      distanceKm: 2.4, deliveryFee: 9.8, deliversHere: true, freeDelivery: false } as Supplier;
    fixture.detectChanges();

    expect(linhaEntrega()!.textContent).toContain('2,4 km');
    expect(linhaEntrega()!.textContent).toMatch(/Entrega\s+R\$\s*9,80/);
  });

  it('deveMostrarSoRetiradaForaDoRaio', () => {
    component.supplier = { id: '7', name: 'Gás Centro', offersDelivery: true, offersPickup: true,
      distanceKm: 9.1, deliveryFee: null, deliversHere: false, freeDelivery: false } as Supplier;
    fixture.detectChanges();

    expect(linhaEntrega()!.textContent).toContain('Só retirada');
    expect(linhaEntrega()!.textContent).toContain('9,1 km');
  });

  it('deveMostrarSeloEntregaGratis', () => {
    component.supplier = { id: '7', name: 'Gás Centro', offersDelivery: true, offersPickup: true,
      distanceKm: 1.2, deliveryFee: 0, deliversHere: true, freeDelivery: true } as Supplier;
    fixture.detectChanges();

    expect(linhaEntrega()!.textContent).toContain('Entrega grátis');
  });

  it('deveAvisarDistanciaNaoCalculada', () => {
    component.supplier = { id: '7', name: 'Gás Centro', offersDelivery: true, offersPickup: true,
      distanceKm: null, deliveryFee: 5, deliversHere: true, freeDelivery: false } as Supplier;
    fixture.detectChanges();

    expect(linhaEntrega()!.textContent).toMatch(/Entrega\s+R\$\s*5,00/);
    expect(linhaEntrega()!.textContent).toContain('distância não calculada');
  });

  it('deveMostrarSoRetiradaQuandoALojaNaoEntrega', () => {
    component.supplier = { id: '7', name: 'Gás Centro', offersDelivery: false, offersPickup: true,
      freeDelivery: false } as Supplier;
    fixture.detectChanges();

    expect(linhaEntrega()!.textContent).toContain('Só retirada');
  });

  it('naoDeveMostrarFreteSemCep', () => {
    component.supplier = { id: '7', name: 'Gás Centro', offersDelivery: true, offersPickup: true,
      distanceKm: null, deliveryFee: null, deliversHere: null, freeDelivery: false } as Supplier;
    fixture.detectChanges();

    expect(linhaEntrega()).toBeNull();
  });
});
