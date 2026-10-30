import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { ToastrService } from 'ngx-toastr';
import { AdminSettingsComponent } from './admin-settings.component';
import { AdminSettingsService, AppSettings } from '../../../services/admin-settings.service';

// HU26: o admin liga e desliga as mensagens de WhatsApp
describe('AdminSettingsComponent', () => {
  let fixture: ComponentFixture<AdminSettingsComponent>;
  let service: jasmine.SpyObj<AdminSettingsService>;
  let toastr: jasmine.SpyObj<ToastrService>;

  const config = (configured: boolean, login = false, order = false): AppSettings => ({
    whatsappConfigured: configured,
    settings: [{ key: 'WHATSAPP_LOGIN_CODE', enabled: login }, { key: 'WHATSAPP_ORDER_OUT_FOR_DELIVERY', enabled: order }],
  });
  const byId = (id: string) => fixture.nativeElement.querySelector(`[data-testid="${id}"]`) as HTMLInputElement | null;

  async function criar(c: AppSettings) {
    service = jasmine.createSpyObj('AdminSettingsService', ['list', 'update']);
    service.list.and.returnValue(of(c));
    toastr = jasmine.createSpyObj('ToastrService', ['success', 'error']);
    await TestBed.configureTestingModule({
      imports: [AdminSettingsComponent],
      providers: [{ provide: AdminSettingsService, useValue: service }, { provide: ToastrService, useValue: toastr }],
    }).compileComponents();
    fixture = TestBed.createComponent(AdminSettingsComponent);
    fixture.detectChanges();
  }

  it('deveListarOsInterruptores', async () => {
    await criar(config(true, false, true));

    expect(byId('setting-WHATSAPP_LOGIN_CODE')!.checked).toBeFalse();
    expect(byId('setting-WHATSAPP_ORDER_OUT_FOR_DELIVERY')!.checked).toBeTrue();
    expect(fixture.nativeElement.textContent).toContain('Código de login por WhatsApp');
    expect(byId('twilio-missing')).toBeNull();
  });

  it('deveLigarEDesligar', async () => {
    await criar(config(true));
    service.update.and.returnValue(of({ key: 'WHATSAPP_LOGIN_CODE', enabled: true }));

    byId('setting-WHATSAPP_LOGIN_CODE')!.click();
    fixture.detectChanges();

    expect(service.update).toHaveBeenCalledWith('WHATSAPP_LOGIN_CODE', true);
    expect(byId('setting-WHATSAPP_LOGIN_CODE')!.checked).toBeTrue();
    expect(toastr.success).toHaveBeenCalled();
  });

  it('deveVoltarOInterruptorSeOBackendRecusar', async () => {
    await criar(config(true));
    service.update.and.returnValue(throwError(() => ({ error: { error: 'Twilio não configurado' } })));

    byId('setting-WHATSAPP_LOGIN_CODE')!.click();
    fixture.detectChanges();

    expect(byId('setting-WHATSAPP_LOGIN_CODE')!.checked).toBeFalse();
    expect(toastr.error).toHaveBeenCalled();
  });

  it('deveTravarSemTwilio', async () => {
    await criar(config(false));

    expect(byId('twilio-missing')).not.toBeNull();
    expect(byId('setting-WHATSAPP_LOGIN_CODE')!.disabled).toBeTrue();
    expect(byId('setting-WHATSAPP_ORDER_OUT_FOR_DELIVERY')!.disabled).toBeTrue();
  });

  it('devePermitirDesligarSemTwilioOQueFicouLigado', async () => {
    await criar(config(false, true));

    expect(byId('setting-WHATSAPP_LOGIN_CODE')!.disabled).toBeFalse();
  });
});
