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

  it('deveNavegarParaInicioComViewProductsAoClicarEmCategorias', () => {
    (fixture.nativeElement.querySelector('[data-testid="tab-categorias"]') as HTMLButtonElement).click();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/inicio'], { queryParams: { view: 'products' } });
  });

  it('deveNavegarParaMeusPedidosAoClicarEmPedidos', () => {
    (fixture.nativeElement.querySelector('[data-testid="tab-pedidos"]') as HTMLButtonElement).click();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/meus-pedidos'], { queryParams: {} });
  });

  it('deveNavegarParaPainelClienteAoClicarEmPerfil', () => {
    (fixture.nativeElement.querySelector('[data-testid="tab-perfil"]') as HTMLButtonElement).click();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/painel-cliente'], { queryParams: {} });
  });
});
