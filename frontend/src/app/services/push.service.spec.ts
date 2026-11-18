import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { SwPush } from '@angular/service-worker';
import { BehaviorSubject } from 'rxjs';
import { PushService } from './push.service';
import { environment } from '../../environments/environment';

// HU27: assinatura de push do aparelho
describe('PushService', () => {
  let service: PushService;
  let http: HttpTestingController;
  let subscription$: BehaviorSubject<PushSubscription | null>;
  let swPush: { isEnabled: boolean; subscription: any; requestSubscription: jasmine.Spy; unsubscribe: jasmine.Spy };

  const assinatura = {
    endpoint: 'https://fcm.googleapis.com/fcm/send/abc',
    toJSON: () => ({ endpoint: 'https://fcm.googleapis.com/fcm/send/abc', keys: { p256dh: 'pk', auth: 'au' } }),
  } as unknown as PushSubscription;

  function criar(enabled = true, permission: NotificationPermission = 'default') {
    subscription$ = new BehaviorSubject<PushSubscription | null>(null);
    swPush = {
      isEnabled: enabled,
      subscription: subscription$.asObservable(),
      requestSubscription: jasmine.createSpy('requestSubscription').and.resolveTo(assinatura),
      unsubscribe: jasmine.createSpy('unsubscribe').and.resolveTo(),
    };
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), { provide: SwPush, useValue: swPush }],
    });
    service = TestBed.inject(PushService);
    http = TestBed.inject(HttpTestingController);
    spyOn(service, 'permission').and.returnValue(permission);
  }

  beforeEach(() => localStorage.removeItem('h2gas.push.dismissed'));
  afterEach(() => http.verify());

  it('deveAtivarEGravarAAssinatura', async () => {
    criar();
    const ativando = service.enable();

    http.expectOne(`${environment.apiUrl}/push/public-key`).flush({ publicKey: 'BPublica', configured: true });
    await Promise.resolve();
    await Promise.resolve();
    const post = http.expectOne(`${environment.apiUrl}/push/subscriptions`);
    expect(post.request.method).toBe('POST');
    expect(post.request.body).toEqual({ endpoint: 'https://fcm.googleapis.com/fcm/send/abc', keys: { p256dh: 'pk', auth: 'au' } });
    post.flush(null);

    await expectAsync(ativando).toBeResolvedTo(true);
    expect(swPush.requestSubscription).toHaveBeenCalledWith({ serverPublicKey: 'BPublica' });
  });

  it('naoDeveAtivarSemConfiguracaoNoServidor', async () => {
    criar();
    const ativando = service.enable();

    http.expectOne(`${environment.apiUrl}/push/public-key`).flush({ publicKey: '', configured: false });

    await expectAsync(ativando).toBeResolvedTo(false);
    expect(swPush.requestSubscription).not.toHaveBeenCalled();
  });

  it('naoDevePerguntarSemSuporteOuNegado', () => {
    criar(false);
    expect(service.canAsk()).toBeFalse();
    TestBed.resetTestingModule();
    criar(true, 'denied');
    expect(service.canAsk()).toBeFalse();
    TestBed.resetTestingModule();
    criar(true, 'default');
    expect(service.canAsk()).toBeTrue();
  });

  it('deveLembrarOAgoraNao', () => {
    criar();
    service.dismiss();
    expect(service.canAsk()).toBeFalse();
  });

  it('deveDesativarNoLogoutComOTokenDaHoraDaChamada', async () => {
    criar();
    subscription$.next(assinatura);
    localStorage.setItem('access_token', 'jwt-de-quem-saiu');

    const desativando = service.disable();
    localStorage.removeItem('access_token'); // o logout apaga o token logo depois
    await Promise.resolve();

    const del = http.expectOne(`${environment.apiUrl}/push/subscriptions`);
    expect(del.request.method).toBe('DELETE');
    expect(del.request.headers.get('Authorization')).toBe('Bearer jwt-de-quem-saiu');
    expect(del.request.body).toEqual({ endpoint: 'https://fcm.googleapis.com/fcm/send/abc' });
    del.flush(null);
    await desativando;
    expect(swPush.unsubscribe).toHaveBeenCalled();
  });

  it('deveRegravarAAssinaturaSePermissaoJaFoiDada', async () => {
    criar(true, 'granted');
    const sincronizando = service.syncIfGranted();

    http.expectOne(`${environment.apiUrl}/push/public-key`).flush({ publicKey: 'BPublica', configured: true });
    await Promise.resolve();
    await Promise.resolve();
    http.expectOne(`${environment.apiUrl}/push/subscriptions`).flush(null);
    await sincronizando;
    expect(swPush.requestSubscription).toHaveBeenCalled();
  });

  it('naoDeveFazerNadaSemServiceWorker', async () => {
    criar(false);
    await service.disable();
    await service.syncIfGranted();
    expect(swPush.unsubscribe).not.toHaveBeenCalled();
  });
});
