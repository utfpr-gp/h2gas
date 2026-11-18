import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CategoryChipBarComponent } from './category-chip-bar.component';

describe('CategoryChipBarComponent', () => {
  let component: CategoryChipBarComponent;
  let fixture: ComponentFixture<CategoryChipBarComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CategoryChipBarComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(CategoryChipBarComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  // ---- HU15: o chip emite o tipo inteiro — lojas filtram por id, produtos por nome ----
  it('deveEmitirOTipoInteiro', () => {
    component.categories = [{ id: 1, name: 'Gás' }, { id: 2, name: 'Água' }];
    fixture.detectChanges();
    spyOn(component.categorySelected, 'emit');

    const chipAgua = Array.from(fixture.nativeElement.querySelectorAll('button') as HTMLButtonElement[])
      .find((b) => b.textContent?.includes('Água'));
    chipAgua?.click();

    expect(component.categorySelected.emit).toHaveBeenCalledWith(jasmine.objectContaining({ id: 2, name: 'Água' }));
  });

  it('deveEmitirNullAoClicarEmTodas', () => {
    component.categories = [{ id: 1, name: 'Gás' }];
    fixture.detectChanges();
    spyOn(component.categorySelected, 'emit');

    const chipTodas = Array.from(fixture.nativeElement.querySelectorAll('button') as HTMLButtonElement[])
      .find((b) => b.textContent?.includes('Todas'));
    chipTodas?.click();

    expect(component.categorySelected.emit).toHaveBeenCalledWith(null);
  });

  it('deveMostrarOIconeDoTipo', () => {
    component.categories = [{ id: 1, name: 'Gelo', icon: 'ac_unit' }, { id: 2, name: 'Diversos' }];
    fixture.detectChanges();

    const icones = Array.from(fixture.nativeElement.querySelectorAll('button .material-icons') as NodeListOf<HTMLElement>)
      .map((el) => el.textContent?.trim());
    expect(icones).toEqual(['apps', 'ac_unit', 'category']);
  });

  it('deveDestacarOChipAtivoComAccent', () => {
    component.categories = [{ id: 1, name: 'Gás', icon: 'local_fire_department' }] as any;
    component.selected = 'Gás';
    fixture.detectChanges();

    const chip = fixture.nativeElement.querySelector('[data-testid="chip-Gás"]') as HTMLElement;
    const circulo = chip.querySelector('span') as HTMLElement;
    const rotulo = chip.querySelectorAll('span')[2] as HTMLElement;
    expect(circulo.classList).toContain('bg-accent');
    expect(circulo.classList).toContain('border-accent');
    expect(circulo.classList).toContain('text-white');
    expect(rotulo.classList).toContain('text-accent');
    expect(rotulo.classList).toContain('font-semibold');
    expect(chip.getAttribute('aria-pressed')).toBe('true');
  });

  it('naoDeveDestacarOChipInativo', () => {
    component.categories = [{ id: 1, name: 'Gás', icon: 'local_fire_department' }] as any;
    component.selected = null;
    fixture.detectChanges();

    const circulo = fixture.nativeElement.querySelector('[data-testid="chip-Gás"] span') as HTMLElement;
    expect(circulo.classList).not.toContain('bg-accent');
  });
});
