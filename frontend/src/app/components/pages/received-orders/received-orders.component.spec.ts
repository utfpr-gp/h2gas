import { Subject } from 'rxjs';
import { SupplierLiveService, SupplierLiveEvent } from '../../../services/supplier-live.service';
import { PushService } from '../../../services/push.service';
import { ComponentFixture, TestBed, discardPeriodicTasks, fakeAsync, tick } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { Router, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { ToastrService } from 'ngx-toastr';

import { ReceivedOrdersComponent } from './received-orders.component';
import { OrderService } from '../../../services/order.service';
import { UserService } from '../../../services/user.service';
import { SupplierService } from '../../../services/supplier.service';
import { DelivererService } from '../../../services/deliverer.service';
import { Deliverer } from '../../../models/deliverer';
import { OrderResponse } from '../../../models/order.model';

describe('ReceivedOrdersComponent', () => {
  // HU27 (tempo real da loja): aviso do canal SSE da loja
  let live$ = new Subject<SupplierLiveEvent>();
  beforeEach(() => (live$ = new Subject<SupplierLiveEvent>()));

  let component: ReceivedOrdersComponent;
  let fixture: ComponentFixture<ReceivedOrdersComponent>;
  let orderServiceSpy: jasmine.SpyObj<OrderService>;
  let toastrSpy: jasmine.SpyObj<ToastrService>;

  let delivererServiceSpy: jasmine.SpyObj<DelivererService>;

  const carlos: Deliverer = { id: 1, name: 'Carlos Silva', email: 'c@h2gas.local', phoneNumber: '41988887777', vehicleInfo: 'Moto', active: true, busy: true };
  const ana: Deliverer = { id: 2, name: 'Ana Souza', email: 'a@h2gas.local', phoneNumber: '41977776666', vehicleInfo: 'Carro', active: true, busy: false };

  const pedido = (id: string, status: string, deliveryMode = 'DELIVERY', extra: Partial<OrderResponse> = {}) =>
    ({ id, status, deliveryMode, supplierName: 'Gás Centro', customerName: 'Maria', totalPrice: 125, orderItems: [], ...extra } as unknown as OrderResponse);
  const lista = (...orders: OrderResponse[]) =>
    of({ data: orders, page: 0, totalPages: 1, hasNext: false, hasPrevious: false } as any);

  beforeEach(async () => {
    orderServiceSpy = jasmine.createSpyObj('OrderService',
      ['findByExcludeStatus', 'findBySupplierId', 'updateStatusOrder', 'cancelOrder', 'assignDeliverer', 'getMyStatusCounts']);
    // HU23: contagem por status vinda do backend
    orderServiceSpy.getMyStatusCounts.and.returnValue(of({
      counts: { PENDING: 2, CONFIRMED: 1, IN_PREPARATION: 0, OUT_FOR_DELIVERY: 1, DELIVERED: 7, CANCELED: 1 },
      active: 4, total: 12, receivable: 335.88,
    }));
    orderServiceSpy.findBySupplierId.and.returnValue(lista(pedido('e1', 'Entregue')));
    orderServiceSpy.findByExcludeStatus.and.returnValue(
      lista(pedido('c1', 'Confirmado'), pedido('p1', 'Pendente'), pedido('p2', 'Pendente')));
    orderServiceSpy.updateStatusOrder.and.returnValue(of({ status: 'Confirmado' }));
    orderServiceSpy.cancelOrder.and.returnValue(of({ status: 'Cancelado', customerNotified: true }));
    delivererServiceSpy = jasmine.createSpyObj('DelivererService', ['listMine']);
    delivererServiceSpy.listMine.and.returnValue(of([carlos, ana]));
    toastrSpy = jasmine.createSpyObj('ToastrService', ['success', 'warning', 'error', 'info']);
    const userServiceSpy = jasmine.createSpyObj('UserService', ['getUserLocalStorage']);
    userServiceSpy.getUserLocalStorage.and.returnValue({ id: 'sup-1', supplierId: 'sup-1' });
    const supplierServiceSpy = jasmine.createSpyObj('SupplierService', ['getOpen', 'getMySettings']);
    supplierServiceSpy.getOpen.and.returnValue(of(true));
    supplierServiceSpy.getMySettings.and.returnValue(of({ openingTime: '08:00', closingTime: '18:00', averageDeliveryTime: 30, lowStockThreshold: 8 }));

    await TestBed.configureTestingModule({
      imports: [ReceivedOrdersComponent],
      providers: [
        { provide: SupplierLiveService, useValue: { changes: () => live$.asObservable() } },
        { provide: PushService, useValue: jasmine.createSpyObj('PushService', { canAsk: false, syncIfGranted: Promise.resolve(), enable: Promise.resolve(false), dismiss: undefined }) }, // HU27
        { provide: OrderService, useValue: orderServiceSpy },
        { provide: UserService, useValue: userServiceSpy },
        { provide: SupplierService, useValue: supplierServiceSpy },
        { provide: DelivererService, useValue: delivererServiceSpy },
        { provide: ToastrService, useValue: toastrSpy },
        provideRouter([]),
        provideNoopAnimations(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ReceivedOrdersComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => fixture.destroy());

  it('should create', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  // F9: "Editar loja" abre as configurações da loja (o /minha-loja agora é o hub)
  it('deveLevarEditarLojaParaAsConfiguracoes', () => {
    const router = TestBed.inject(Router);
    const navegar = spyOn(router, 'navigate').and.resolveTo(true);

    component.goToStore();

    expect(navegar).toHaveBeenCalledWith(['/minha-loja/configuracoes']);
  });

  it('deveSepararPedidosPendentesNoTopoComContagem', () => {
    fixture.detectChanges();

    expect(component.pendingOrders.map((o) => o.id)).toEqual(['p1', 'p2']);
    expect(component.otherOrders.map((o) => o.id)).toEqual(['c1']);
  });

  it('deveSelecionarPrimeiroPendentePorPadrao', () => {
    fixture.detectChanges();

    expect(component.selectedOrder?.id).toBe('p1');
  });

  it('deveConfirmarPedidoPendenteComUmClique', () => {
    fixture.detectChanges();
    const p1 = component.pendingOrders[0];

    component.changeStatus(p1, 'CONFIRMED');

    expect(orderServiceSpy.updateStatusOrder).toHaveBeenCalledOnceWith({ orderStatus: 'CONFIRMED' }, 'p1');
    expect(p1.status).toBe('Confirmado');
  });

  it('naoDevePermitirCancelarSemJustificativa', () => {
    fixture.detectChanges();
    component.openCancel(component.pendingOrders[0]);
    component.cancelReason = '   ';

    component.confirmCancel();

    expect(orderServiceSpy.cancelOrder).not.toHaveBeenCalled();
    expect(component.cancelTarget?.id).toBe('p1');
  });

  it('deveChamarCancelOrderComMotivoInformado', () => {
    fixture.detectChanges();
    component.openCancel(component.pendingOrders[0]);
    component.cancelReason = '  Sem gás no estoque  ';

    component.confirmCancel();

    expect(orderServiceSpy.cancelOrder).toHaveBeenCalledOnceWith('p1', 'Sem gás no estoque');
    expect(component.orders?.map((o) => o.id)).not.toContain('p1');
    expect(component.cancelTarget).toBeUndefined();
    expect(toastrSpy.success).toHaveBeenCalled();
  });

  it('deveAvisarQuandoClienteNaoFoiNotificado', () => {
    orderServiceSpy.cancelOrder.and.returnValue(of({ status: 'Cancelado', customerNotified: false }));
    fixture.detectChanges();
    component.openCancel(component.pendingOrders[0]);
    component.cancelReason = 'Sem entregador';

    component.confirmCancel();

    expect(toastrSpy.warning).toHaveBeenCalledWith(jasmine.stringMatching('não foi avisado'));
  });

  it('deveAtualizarPedidosPeriodicamente', fakeAsync(() => {
    fixture.detectChanges();
    expect(orderServiceSpy.findByExcludeStatus).toHaveBeenCalledTimes(1);

    orderServiceSpy.findByExcludeStatus.and.returnValue(
      lista(pedido('p3', 'Pendente'), pedido('c1', 'Confirmado'), pedido('p1', 'Pendente'), pedido('p2', 'Pendente')));
    tick(30000);

    expect(orderServiceSpy.findByExcludeStatus).toHaveBeenCalledTimes(2);
    expect(component.isNew(component.orders!.find((o) => o.id === 'p3')!)).toBeTrue();
    expect(component.isNew(component.orders!.find((o) => o.id === 'p1')!)).toBeFalse();
    expect(component.selectedOrder?.id).toBe('p1');

    fixture.destroy();
    discardPeriodicTasks();
  }));

  it('naoDeveExibirToastDeSucessoAoBuscarPedidos', () => {
    fixture.detectChanges();

    expect(toastrSpy.success).not.toHaveBeenCalled();
  });

  it('deveExibirContagemDePendentesNoBadge', () => {
    fixture.detectChanges();

    const badge = fixture.nativeElement.querySelector('[data-testid=pending-count]') as HTMLElement;
    expect(badge.textContent?.trim()).toBe('2');
    expect(fixture.nativeElement.querySelectorAll('[data-testid=order-row]').length).toBe(3);
  });

  // HU28 Cenário 4: o detalhe mostra subtotal e frete
  it('deveMostrarOFreteNoDetalhe', () => {
    fixture.detectChanges();

    expect(component.selectedOrder?.deliveryMode).toBe('DELIVERY');
    expect(fixture.nativeElement.querySelector('[data-testid="order-fee"]')).not.toBeNull();
  });

  // HU28 Cenário 2: "A receber" vem do backend (pedidos em andamento), não da lista filtrada
  it('deveMostrarAReceberDoBackend', () => {
    fixture.detectChanges();

    const card = fixture.nativeElement.querySelector('[data-testid=receivable]') as HTMLElement;
    expect(card.textContent).toMatch(/335[.,]88/);
  });

  it('deveAbrirModalDeJustificativaComBotaoTravado', () => {
    fixture.detectChanges();

    (fixture.nativeElement.querySelector('[data-testid=action-CANCELED]') as HTMLButtonElement).click();
    fixture.detectChanges();

    const modal = fixture.nativeElement.querySelector('[data-testid=cancel-modal]') as HTMLElement;
    expect(modal).toBeTruthy();
    expect((modal.querySelector('[data-testid=confirm-cancel]') as HTMLButtonElement).disabled).toBeTrue();
    expect(orderServiceSpy.updateStatusOrder).not.toHaveBeenCalled();
  });

  describe('despacho (HU07)', () => {
    const emPreparo = () => lista(
      pedido('e1', 'Em preparacao', 'DELIVERY'),
      pedido('r1', 'Em preparacao', 'PICKUP'));

    beforeEach(() => {
      orderServiceSpy.findByExcludeStatus.and.returnValue(emPreparo());
      orderServiceSpy.updateStatusOrder.and.returnValue(of({ status: 'Saiu para entrega' }));
      orderServiceSpy.assignDeliverer.and.returnValue(of({ id: 'e1', status: 'Em preparacao', delivererName: 'Ana Souza' } as any));
      fixture.detectChanges();
    });

    const botao = (acao: string) => fixture.nativeElement.querySelector(`[data-testid=action-${acao}]`) as HTMLButtonElement | null;

    it('deveExibirDespacharApenasParaEntregaEmPreparo', () => {
      component.selectOrder(component.orders!.find((o) => o.id === 'e1'));
      fixture.detectChanges();
      expect(botao('DISPATCH')?.textContent?.trim()).toBe('Despachar');
      expect(botao('OUT_FOR_DELIVERY')).toBeNull();

      component.selectOrder(component.orders!.find((o) => o.id === 'r1'));
      fixture.detectChanges();
      expect(botao('DISPATCH')).toBeNull();
      expect(botao('OUT_FOR_DELIVERY')?.textContent?.trim()).toBe('Pronto para retirada');
    });

    it('deveManterAtalhoDeRetiradaSemEntregador', () => {
      component.selectOrder(component.orders!.find((o) => o.id === 'r1'));
      fixture.detectChanges();

      botao('OUT_FOR_DELIVERY')!.click();
      fixture.detectChanges();

      expect(orderServiceSpy.updateStatusOrder).toHaveBeenCalledOnceWith({ orderStatus: 'OUT_FOR_DELIVERY' }, 'r1');
      expect(fixture.nativeElement.querySelector('[data-testid=dispatch-modal]')).toBeNull();
    });

    it('deveAtribuirEntregadorSelecionadoNoModal', () => {
      component.selectOrder(component.orders!.find((o) => o.id === 'e1'));
      fixture.detectChanges();
      botao('DISPATCH')!.click();
      fixture.detectChanges();

      const opcoes = Array.from(fixture.nativeElement.querySelectorAll('[data-testid=deliverer-option]')) as HTMLElement[];
      expect(opcoes.length).toBe(2);
      expect(opcoes[0].textContent).toContain('Em rota');
      expect(opcoes[1].textContent).toContain('Disponível');
      const confirmar = () => fixture.nativeElement.querySelector('[data-testid=confirm-dispatch]') as HTMLButtonElement;
      expect(confirmar().disabled).toBeTrue();

      opcoes[1].click();
      fixture.detectChanges();
      confirmar().click();
      fixture.detectChanges();

      expect(orderServiceSpy.assignDeliverer).toHaveBeenCalledOnceWith('e1', 2);
      expect(component.orders!.find((o) => o.id === 'e1')!.status).toBe('Em preparacao');
      expect(component.orders!.find((o) => o.id === 'e1')!.delivererName).toBe('Ana Souza');
      expect(fixture.nativeElement.querySelector('[data-testid=dispatch-modal]')).toBeNull();
      expect(toastrSpy.success).toHaveBeenCalledWith(jasmine.stringMatching('Ana Souza'));
    });

    it('deveOferecerTrocarEntregadorQuandoJaAtribuido', () => {
      orderServiceSpy.findByExcludeStatus.and.returnValue(lista(
        pedido('e1', 'Em preparacao', 'DELIVERY', { delivererName: 'Carlos Silva' })));
      component.ngOnInit();
      component.selectOrder(component.orders!.find((o) => o.id === 'e1'));
      fixture.detectChanges();

      expect(botao('DISPATCH')?.textContent?.trim()).toBe('Trocar entregador');
      expect(fixture.nativeElement.textContent).toContain('Aguardando Carlos Silva iniciar a entrega');
    });

    it('deveExibirMotivoDaUltimaRecusa', () => {
      orderServiceSpy.findByExcludeStatus.and.returnValue(lista(
        pedido('e1', 'Em preparacao', 'DELIVERY', { lastRefusalReason: 'Moto quebrou', lastRefusalBy: 'Carlos Silva' })));
      component.ngOnInit();
      component.selectOrder(component.orders!.find((o) => o.id === 'e1'));
      fixture.detectChanges();

      const aviso = fixture.nativeElement.querySelector('[data-testid=last-refusal]') as HTMLElement;
      expect(aviso.textContent).toContain('Recusado por Carlos Silva');
      expect(aviso.textContent).toContain('Moto quebrou');
      expect(botao('DISPATCH')?.textContent?.trim()).toBe('Despachar');
    });

    it('deveAvisarQuandoNaoHaEntregadores', () => {
      delivererServiceSpy.listMine.and.returnValue(of([]));
      component.selectOrder(component.orders!.find((o) => o.id === 'e1'));
      fixture.detectChanges();

      botao('DISPATCH')!.click();
      fixture.detectChanges();

      expect(fixture.nativeElement.querySelector('[data-testid=no-deliverers]')).toBeTruthy();
    });
  });

  // ---- HU18 ----
  it('deveMostrarOEstadoDaLojaSemInterruptor', () => {
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('app-store-hours-status')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="store-toggle"]')).toBeNull();
  });

  // ---- HU23: filtro com todos os status e a contagem do backend ----
  const filtro = (valor: string) => fixture.nativeElement.querySelector(`[data-testid="status-filter-${valor}"]`) as HTMLButtonElement;

  it('deveMostrarTodosOsStatusComContagem', () => {
    fixture.detectChanges();
    const esperado: [string, string, string][] = [
      ['ALL', 'Em andamento', '4'], ['EVERYTHING', 'Todos', '12'], ['PENDING', 'Pendentes', '2'],
      ['CONFIRMED', 'Confirmados', '1'], ['IN_PREPARATION', 'Em preparo', '0'], ['OUT_FOR_DELIVERY', 'Em rota', '1'],
      ['DELIVERED', 'Entregues', '7'], ['CANCELED', 'Cancelados', '1'],
    ];
    for (const [valor, rotulo, numero] of esperado) {
      const botao = filtro(valor);
      expect(botao).withContext(valor).not.toBeNull();
      expect(botao.textContent).withContext(valor).toContain(rotulo);
      expect(botao.querySelector('[data-testid="status-count"]')?.textContent?.trim()).withContext(valor).toBe(numero);
    }
    expect(filtro('ALL').getAttribute('aria-pressed')).toBe('true');
  });

  it('deveFiltrarPeloStatusEscolhido', () => {
    fixture.detectChanges();
    filtro('DELIVERED').click();
    fixture.detectChanges();

    expect(orderServiceSpy.findBySupplierId.calls.mostRecent().args.slice(1, 4)).toEqual(['DELIVERED', 0, 50]);
    expect(filtro('DELIVERED').getAttribute('aria-pressed')).toBe('true');
    expect(filtro('ALL').getAttribute('aria-pressed')).toBe('false');

    filtro('EVERYTHING').click();
    fixture.detectChanges();
    expect(orderServiceSpy.findBySupplierId.calls.mostRecent().args.slice(1, 4)).toEqual([undefined, 0, 50]);
  });

  it('deveRecarregarAContagemJuntoComALista', () => {
    fixture.detectChanges();
    const antes = orderServiceSpy.getMyStatusCounts.calls.count();

    filtro('PENDING').click();

    expect(orderServiceSpy.getMyStatusCounts.calls.count()).toBe(antes + 1);
  });

  // ---- HU25 ----
  it('deveAbrirOWhatsappDoClienteComMensagem', () => {
    orderServiceSpy.findByExcludeStatus.and.returnValue(lista(
      pedido('4a1a4ffe-p1', 'Pendente', 'DELIVERY', { customerName: 'Maria Souza', customerPhone: '(42) 99999-0000' })));
    fixture.detectChanges();

    const link = fixture.nativeElement.querySelector('[data-testid="whatsapp-customer"]') as HTMLAnchorElement;
    expect(link.getAttribute('href')).toBe('https://wa.me/5542999990000?text='
      + encodeURIComponent('Olá, Maria! Aqui é a Gás Centro, sobre o seu pedido #4a1a4ffe no H2Gás.'));
    expect(link.getAttribute('rel')).toContain('noopener');
    expect(link.querySelector('app-whatsapp-icon svg')).not.toBeNull(); // logo do WhatsApp no botão
  });

  // HU28 Cenário 5: telefone legível; o tel: segue com os dígitos
  it('deveFormatarOTelefoneDoCliente', () => {
    orderServiceSpy.findByExcludeStatus.and.returnValue(lista(
      pedido('p1', 'Pendente', 'DELIVERY', { customerPhone: '42990000011' })));
    fixture.detectChanges();

    const tel = fixture.nativeElement.querySelector('[data-testid="customer-phone"]') as HTMLAnchorElement;
    expect(tel.textContent?.trim()).toBe('(42) 99000-0011');
    expect(tel.getAttribute('href')).toBe('tel:42990000011');
  });

  it('naoDeveMostrarWhatsappDoClienteNoPedidoEncerrado', () => {
    orderServiceSpy.findBySupplierId.and.returnValue(lista(
      pedido('e1', 'Entregue', 'DELIVERY', { customerPhone: '(42) 99999-0000' })));
    fixture.detectChanges();
    component.onStatusChange('EVERYTHING');
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="whatsapp-customer"]')).toBeNull();
  });

  // ---- HU28 Cenário 6 e 7: data e pedido parado ----
  it('deveMostrarADataQuandoOPedidoNaoEDeHoje', () => {
    const hoje = new Date();
    const antigo = new Date(hoje.getFullYear(), hoje.getMonth(), hoje.getDate() - 6, 19, 32);
    orderServiceSpy.findByExcludeStatus.and.returnValue(lista(
      pedido('antigo', 'Em rota', 'DELIVERY', { orderDate: antigo }),
      pedido('hoje', 'Pendente', 'DELIVERY', { orderDate: new Date(hoje.getFullYear(), hoje.getMonth(), hoje.getDate(), 8, 5) })));
    fixture.detectChanges();

    const horas = Array.from(fixture.nativeElement.querySelectorAll('[data-testid=order-time]')) as HTMLElement[];
    const textos = horas.map(h => h.textContent?.trim());
    const dd = String(antigo.getDate()).padStart(2, '0'), mm = String(antigo.getMonth() + 1).padStart(2, '0');
    expect(textos).toContain(`${dd}/${mm} 19:32`);
    expect(textos).toContain('08:05');
  });

  it('deveDestacarPedidoParadoEAvisarNoTopo', () => {
    orderServiceSpy.findByExcludeStatus.and.returnValue(lista(
      pedido('parado', 'Em rota', 'DELIVERY', { stalled: true, stalledMinutes: 6 * 1440 }),
      pedido('ok', 'Pendente', 'DELIVERY', { stalled: false })));
    fixture.detectChanges();

    const badges = Array.from(fixture.nativeElement.querySelectorAll('[data-testid=stalled-badge]')) as HTMLElement[];
    expect(badges.length).toBe(1);
    expect(badges[0].textContent?.trim()).toBe('parado há 6 dias');
    expect(badges[0].closest('[data-testid=order-row]')!.classList).toContain('border-l-error');
    expect(fixture.nativeElement.querySelector('[data-testid=stalled-alert]')?.textContent).toContain('1 pedido parado');
  });

  it('naoDeveAvisarSemPedidoParado', () => {
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid=stalled-alert]')).toBeNull();
  });

  it('deveRecarregarQuandoChegaPedidoNovo', () => {
    fixture.detectChanges();
    const antes = orderServiceSpy.findByExcludeStatus.calls.count();

    live$.next({ type: 'order-placed', orderId: 'novo-1' });

    expect(orderServiceSpy.findByExcludeStatus.calls.count()).toBe(antes + 1);
    expect(orderServiceSpy.getMyStatusCounts).toHaveBeenCalledTimes(2);
  });
});
