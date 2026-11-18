import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';

import { BottomTabBarComponent } from './bottom-tab-bar.component';

describe('BottomTabBarComponent', () => {
  let component: BottomTabBarComponent;
  let fixture: ComponentFixture<BottomTabBarComponent>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [BottomTabBarComponent],
      providers: [{ provide: Router, useValue: routerSpy }]
    }).compileComponents();

    fixture = TestBed.createComponent(BottomTabBarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('deveNavegarParaInicioAoClicarNoIconeInicio', () => {
    (fixture.nativeElement.querySelector('[data-testid="tab-inicio"]') as HTMLButtonElement).click();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/inicio'], { queryParams: {} });
  });

  // HU15 (revisão mobile): a aba "Categorias" saiu — a visão Produtos fica no seletor Lojas/Produtos da vitrine
  it('naoDeveMostrarAbaDeCategorias', () => {
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="tab-categorias"]')).toBeNull();
    expect(el.textContent).not.toContain('Categorias');

    fixture.componentRef.setInput('visitor', true);
    fixture.detectChanges();
    expect(el.querySelector('[data-testid="tab-categorias"]')).toBeNull();
    expect(el.textContent).not.toContain('Categorias');
  });

  it('deveTerTresAbasParaOClienteEDuasParaOVisitante', () => {
    const abas = () => (fixture.nativeElement as HTMLElement).querySelectorAll('[data-testid^="tab-"]').length;
    expect(abas()).toBe(3);

    fixture.componentRef.setInput('visitor', true);
    fixture.detectChanges();
    expect(abas()).toBe(2);
  });

  it('deveNavegarParaMeusPedidosAoClicarEmPedidos', () => {
    (fixture.nativeElement.querySelector('[data-testid="tab-pedidos"]') as HTMLButtonElement).click();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/meus-pedidos'], { queryParams: {} });
  });

  it('deveLevarAbaPerfilParaMeusDados', () => {
    (fixture.nativeElement.querySelector('[data-testid="tab-perfil"]') as HTMLButtonElement).click();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/meus-dados'], { queryParams: {} });
  });

  // HU13: o visitante vê Início e Entrar (pedidos e perfil pedem conta)
  it('deveMostrarEntrarParaVisitante', () => {
    fixture.componentRef.setInput('visitor', true);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;

    expect(el.querySelector('[data-testid="tab-pedidos"]')).toBeNull();
    expect(el.querySelector('[data-testid="tab-perfil"]')).toBeNull();
    (el.querySelector('[data-testid="tab-entrar"]') as HTMLButtonElement).click();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/login']);
  });
});
