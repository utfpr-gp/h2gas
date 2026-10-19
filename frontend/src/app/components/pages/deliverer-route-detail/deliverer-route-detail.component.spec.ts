import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { BehaviorSubject, of, throwError } from 'rxjs';
import { ToastrService } from 'ngx-toastr';

import { DelivererRouteDetailComponent } from './deliverer-route-detail.component';
import { OrderService } from '../../../services/order.service';
import { OrderResponse } from '../../../models/order.model';

describe('DelivererRouteDetailComponent', () => {
  let component: DelivererRouteDetailComponent;
  let fixture: ComponentFixture<DelivererRouteDetailComponent>;
  let orderServiceSpy: jasmine.SpyObj<OrderService>;
  let toastrSpy: jasmine.SpyObj<ToastrService>;
  let navigateSpy: jasmine.Spy;
  let paramMap$: BehaviorSubject<ReturnType<typeof convertToParamMap>>;

  const pedido = (status: string) => ({
    id: 'r1', status, paymentMethod: 'PIX', streetName: 'Rua das Flores', number: '300', complement: 'Apto 42',
    neighborhood: 'Centro', cityName: 'Curitiba', customerName: 'Maria Souza', customerPhone: '(41) 98888-0000',
    totalPrice: 125, orderItems: [{ quantity: 2, productBrand: 'Ultragaz', productNameCategory: 'Gás 13kg', totalPrice: 250 }],
  } as unknown as OrderResponse);

  const text = () => fixture.nativeElement.textContent as string;
  const byId = (id: string) => fixture.nativeElement.querySelector(`[data-testid=${id}]`) as HTMLElement | null;

  const setup = async (status: string) => {
    orderServiceSpy = jasmine.createSpyObj('OrderService', ['myRoutes', 'startDelivery', 'completeDelivery', 'cancelDelivery']);
    orderServiceSpy.myRoutes.and.returnValue(of([pedido(status)]));
    orderServiceSpy.startDelivery.and.returnValue(of({ id: 'r1', status: 'Saiu para entrega' } as OrderResponse));
    orderServiceSpy.completeDelivery.and.returnValue(of({ id: 'r1', status: 'Entregue' } as OrderResponse));
    orderServiceSpy.cancelDelivery.and.returnValue(of({ id: 'r1', status: 'Em preparacao' } as OrderResponse));
    toastrSpy = jasmine.createSpyObj('ToastrService', ['success', 'warning', 'error']);

    await TestBed.configureTestingModule({
      imports: [DelivererRouteDetailComponent],
      providers: [
        { provide: OrderService, useValue: orderServiceSpy },
        { provide: ToastrService, useValue: toastrSpy },
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { paramMap: paramMap$ = new BehaviorSubject(convertToParamMap({ id: 'r1' })) } },
        provideNoopAnimations(),
      ],
    }).compileComponents();

    navigateSpy = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    fixture = TestBed.createComponent(DelivererRouteDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  };

  afterEach(() => fixture.destroy());

  it('deveExibirEnderecoEContatoDoCliente', async () => {
    await setup('Em preparacao');

    expect(text()).toContain('Rua das Flores, 300');
    expect(text()).toContain('Apto 42');
    expect(text()).toContain('Centro');
    expect(text()).toContain('Curitiba');
    expect(text()).toContain('Maria Souza');
    expect(text()).toContain('2x Ultragaz Gás 13kg');
    expect((byId('call-customer') as HTMLAnchorElement).getAttribute('href')).toBe('tel:41988880000');
    expect((byId('whatsapp-customer') as HTMLAnchorElement).getAttribute('href')).toBe('https://wa.me/5541988880000');
  });

  it('deveMontarMapaELinkAbrirNoMaps', async () => {
    await setup('Em preparacao');

    const endereco = encodeURIComponent('Rua das Flores, 300, Centro, Curitiba');
    expect((byId('route-map') as HTMLIFrameElement).src).toBe(`https://maps.google.com/maps?q=${endereco}&output=embed`);
    expect((byId('open-maps') as HTMLAnchorElement).href).toBe(`https://www.google.com/maps/dir/?api=1&destination=${endereco}&travelmode=driving`);
  });

  it('deveAbrirGoogleMapsComARotaAoClicarNoMapa', async () => {
    await setup('Em preparacao');

    const endereco = encodeURIComponent('Rua das Flores, 300, Centro, Curitiba');
    const areaDoMapa = byId('map-click') as HTMLAnchorElement;
    expect(areaDoMapa.href).toBe(`https://www.google.com/maps/dir/?api=1&destination=${endereco}&travelmode=driving`);
    expect(areaDoMapa.target).toBe('_blank');
    expect(areaDoMapa.getAttribute('aria-label')).toContain('Google Maps');
    expect((byId('open-maps') as HTMLAnchorElement).href).toBe(areaDoMapa.href);
  });

  it('deveIniciarEPoisConfirmarEntrega', async () => {
    await setup('Em preparacao');
    expect(byId('confirm-delivery')).toBeNull();

    (byId('start-delivery') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(orderServiceSpy.startDelivery).toHaveBeenCalledOnceWith('r1');
    expect(byId('start-delivery')).toBeNull();

    (byId('confirm-delivery') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(orderServiceSpy.completeDelivery).toHaveBeenCalledOnceWith('r1');
    expect(navigateSpy).toHaveBeenCalledWith(['/entregas']);
  });

  // 3ª revisão (h): com a entrega em rota não há "voltar"; a saída é confirmar ou cancelar
  it('naoDeveExibirVoltarComEntregaEmRota', async () => {
    await setup('Em preparacao');
    expect(byId('back')).toBeTruthy();

    (byId('start-delivery') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(byId('back')).toBeNull();
  });

  it('deveCarregarOutraEntregaQuandoOParametroMudar', async () => {
    await setup('Saiu para entrega');
    orderServiceSpy.myRoutes.and.returnValue(of([{ ...pedido('Saiu para entrega'), id: 'r2', neighborhood: 'Batel' } as OrderResponse]));

    paramMap$.next(convertToParamMap({ id: 'r2' }));
    fixture.detectChanges();

    expect(component.order?.id).toBe('r2');
    expect(text()).toContain('Batel');
  });

  it('deveManterPendenteComVoltarQuandoBackendRecusarInicio', async () => {
    await setup('Em preparacao');
    orderServiceSpy.startDelivery.and.returnValue(
      throwError(() => ({ status: 400, error: { error: 'Finalize a entrega em rota antes de iniciar outra.' } }))
    );

    (byId('start-delivery') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(toastrSpy.error).toHaveBeenCalledWith('Erro ao iniciar a entrega: Finalize a entrega em rota antes de iniciar outra.');
    expect(byId('start-delivery')).toBeTruthy();
    expect(byId('back')).toBeTruthy();
  });

  it('naoDeveCancelarEntregaSemMotivo', async () => {
    await setup('Saiu para entrega');

    (byId('open-cancel') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(byId('cancel-modal')).toBeTruthy();
    expect((byId('confirm-cancel') as HTMLButtonElement).disabled).toBeTrue();
    component.cancelReason = '   ';
    component.confirmCancel();
    expect(orderServiceSpy.cancelDelivery).not.toHaveBeenCalled();
  });

  it('deveCancelarEntregaComMotivoEVoltarParaLista', async () => {
    await setup('Saiu para entrega');

    component.openCancel();
    component.cancelReason = '  Moto quebrou  ';
    component.confirmCancel();

    expect(orderServiceSpy.cancelDelivery).toHaveBeenCalledOnceWith('r1', 'Moto quebrou');
    expect(navigateSpy).toHaveBeenCalledWith(['/entregas']);
    expect(toastrSpy.success).toHaveBeenCalled();
  });

  it('deveVoltarParaListaQuandoEntregaNaoForDoEntregador', async () => {
    orderServiceSpy = jasmine.createSpyObj('OrderService', ['myRoutes']);
    orderServiceSpy.myRoutes.and.returnValue(of([]));
    toastrSpy = jasmine.createSpyObj('ToastrService', ['success', 'warning', 'error']);
    await TestBed.configureTestingModule({
      imports: [DelivererRouteDetailComponent],
      providers: [
        { provide: OrderService, useValue: orderServiceSpy },
        { provide: ToastrService, useValue: toastrSpy },
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { paramMap: paramMap$ = new BehaviorSubject(convertToParamMap({ id: 'outro' })) } },
        provideNoopAnimations(),
      ],
    }).compileComponents();
    navigateSpy = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    fixture = TestBed.createComponent(DelivererRouteDetailComponent);
    fixture.detectChanges();

    expect(toastrSpy.warning).toHaveBeenCalled();
    expect(navigateSpy).toHaveBeenCalledWith(['/entregas']);
  });
});
