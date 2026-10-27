import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CatalogSortMenuComponent } from './catalog-sort-menu.component';

// HU16: botão "Ordenar" único, à direita, com as opções da visão atual
describe('CatalogSortMenuComponent', () => {
  let component: CatalogSortMenuComponent;
  let fixture: ComponentFixture<CatalogSortMenuComponent>;

  const byId = (id: string) => fixture.nativeElement.querySelector(`[data-testid="${id}"]`) as HTMLElement | null;
  const abrir = () => {
    byId('sort-menu')!.click();
    fixture.detectChanges();
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [CatalogSortMenuComponent] }).compileComponents();

    fixture = TestBed.createComponent(CatalogSortMenuComponent);
    component = fixture.componentInstance;
    component.options = [
      { value: 'name', label: 'Nome (A-Z)' },
      { value: 'rating', label: 'Melhor avaliação' },
    ];
    component.value = 'name';
    fixture.detectChanges();
  });

  it('deveMostrarAOpcaoAtivaNoBotao', () => {
    expect(byId('sort-menu')!.textContent).toContain('Nome (A-Z)');
    expect(byId('sort-menu')!.getAttribute('aria-expanded')).toBe('false');
    expect(byId('sort-menu')!.classList).toContain('min-h-11');
  });

  it('deveListarAsOpcoesDaVisaoAtual', () => {
    abrir();

    const itens = Array.from(fixture.nativeElement.querySelectorAll('[role="menuitemradio"]')) as HTMLElement[];
    expect(itens.map((i) => i.querySelector('[data-testid="sort-label"]')?.textContent?.trim())).toEqual(['Nome (A-Z)', 'Melhor avaliação']);
    expect(byId('sort-menu')!.getAttribute('aria-expanded')).toBe('true');
  });

  it('deveMarcarAOpcaoAtiva', () => {
    abrir();

    expect(byId('sort-name')!.getAttribute('aria-checked')).toBe('true');
    expect(byId('sort-rating')!.getAttribute('aria-checked')).toBe('false');
  });

  it('deveEmitirAOrdenacaoEscolhida', () => {
    spyOn(component.valueChange, 'emit');
    abrir();

    byId('sort-rating')!.click();

    expect(component.valueChange.emit).toHaveBeenCalledOnceWith('rating');
  });

  it('naoDeveEmitirAoEscolherAMesmaOpcao', () => {
    spyOn(component.valueChange, 'emit');
    abrir();

    byId('sort-name')!.click();

    expect(component.valueChange.emit).not.toHaveBeenCalled();
  });

  it('deveFecharAoEscolher', () => {
    abrir();
    byId('sort-rating')!.click();
    fixture.detectChanges();

    expect(byId('sort-options')).toBeNull();
  });

  it('deveFecharNoEsc', () => {
    abrir();
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    fixture.detectChanges();

    expect(byId('sort-options')).toBeNull();
  });

  it('deveFecharAoClicarFora', () => {
    abrir();
    document.body.click();
    fixture.detectChanges();

    expect(byId('sort-options')).toBeNull();
  });
});
