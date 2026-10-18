import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';

import { DelivererTabBarComponent } from './deliverer-tab-bar.component';

describe('DelivererTabBarComponent', () => {
  let fixture: ComponentFixture<DelivererTabBarComponent>;
  let routerSpy: { url: string; navigate: jasmine.Spy };

  const setup = async (url: string) => {
    routerSpy = { url, navigate: jasmine.createSpy('navigate') };
    await TestBed.configureTestingModule({
      imports: [DelivererTabBarComponent],
      providers: [{ provide: Router, useValue: routerSpy }],
    }).compileComponents();
    fixture = TestBed.createComponent(DelivererTabBarComponent);
    fixture.detectChanges();
  };

  const tab = (id: string) => fixture.nativeElement.querySelector(`[data-testid=tab-${id}]`) as HTMLButtonElement | null;

  it('deveExibirAbasEntregasEHistorico', async () => {
    await setup('/entregas');

    expect(tab('entregas')?.textContent).toContain('Entregas');
    expect(tab('historico')?.textContent).toContain('Histórico');
    expect(tab('entregas')?.getAttribute('aria-current')).toBe('page');
    expect(tab('historico')?.getAttribute('aria-current')).toBeNull();
  });

  it('deveNavegarParaOHistorico', async () => {
    await setup('/entregas');

    tab('historico')!.click();

    expect(routerSpy.navigate).toHaveBeenCalledWith(['/entregas/historico']);
  });

  it('deveMarcarHistoricoComoAbaAtiva', async () => {
    await setup('/entregas/historico');

    expect(tab('historico')?.getAttribute('aria-current')).toBe('page');
    expect(tab('entregas')?.getAttribute('aria-current')).toBeNull();
  });

  it('deveOcultarBarraNoDetalheDaEntrega', async () => {
    await setup('/entregas/7e9cf4bd-5acb-4df6-86c1-db67a60d5a12');

    expect(fixture.nativeElement.querySelector('nav')).toBeNull();
  });
});
