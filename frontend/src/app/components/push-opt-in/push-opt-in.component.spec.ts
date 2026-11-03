import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ToastrService } from 'ngx-toastr';
import { PushOptInComponent } from './push-opt-in.component';
import { PushService } from '../../services/push.service';

// HU27: aviso para ativar as notificações
describe('PushOptInComponent', () => {
  let fixture: ComponentFixture<PushOptInComponent>;
  let push: jasmine.SpyObj<PushService>;
  let toastr: jasmine.SpyObj<ToastrService>;

  const el = (id: string) => fixture.nativeElement.querySelector(`[data-testid="${id}"]`) as HTMLElement | null;

  async function criar(audience: 'customer' | 'supplier' | 'deliverer', canAsk = true) {
    push = jasmine.createSpyObj('PushService', ['canAsk', 'enable', 'dismiss', 'syncIfGranted']);
    push.canAsk.and.returnValue(canAsk);
    push.syncIfGranted.and.resolveTo();
    toastr = jasmine.createSpyObj('ToastrService', ['success', 'warning', 'error']);
    await TestBed.configureTestingModule({
      imports: [PushOptInComponent],
      providers: [{ provide: PushService, useValue: push }, { provide: ToastrService, useValue: toastr }],
    }).compileComponents();
    fixture = TestBed.createComponent(PushOptInComponent);
    fixture.componentRef.setInput('audience', audience);
    fixture.detectChanges();
  }

  it('deveMostrarOTextoDoPerfil', async () => {
    await criar('supplier');
    expect(el('push-opt-in')!.textContent).toContain('pedido novo');
    expect(push.syncIfGranted).toHaveBeenCalled();
  });

  it('naoDeveAparecerQuandoNaoPodePerguntar', async () => {
    await criar('customer', false);
    expect(el('push-opt-in')).toBeNull();
  });

  it('deveAtivarESumir', async () => {
    await criar('customer');
    push.enable.and.resolveTo(true);

    (el('push-enable') as HTMLButtonElement).click();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(push.enable).toHaveBeenCalled();
    expect(toastr.success).toHaveBeenCalled();
    expect(el('push-opt-in')).toBeNull();
  });

  it('deveAvisarQuandoNaoAtivar', async () => {
    await criar('deliverer');
    push.enable.and.resolveTo(false);
    push.canAsk.and.returnValue(false); // o usuário negou no aviso do navegador

    (el('push-enable') as HTMLButtonElement).click();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(toastr.warning).toHaveBeenCalled();
    expect(el('push-opt-in')).toBeNull();
  });

  it('deveDispensarComAgoraNao', async () => {
    await criar('customer');

    (el('push-dismiss') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(push.dismiss).toHaveBeenCalled();
    expect(el('push-opt-in')).toBeNull();
  });
});
