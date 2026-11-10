import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { ToastrService } from 'ngx-toastr';
import { AdminSettingsComponent } from './admin-settings.component';
import { AdminSettingsService, AppSettings } from '../../../services/admin-settings.service';

// HU26: o admin liga e desliga as mensagens ao celular e escolhe WhatsApp ou SMS
describe('AdminSettingsComponent', () => {
  let fixture: ComponentFixture<AdminSettingsComponent>;
  let service: jasmine.SpyObj<AdminSettingsService>;
  let toastr: jasmine.SpyObj<ToastrService>;

  const config = (whatsapp: boolean, sms: boolean, login = false, order = false, useSms = false): AppSettings => ({
    whatsappConfigured: whatsapp,
    smsConfigured: sms,
    settings: [
      { key: 'PHONE_LOGIN_CODE', enabled: login },
      { key: 'PHONE_ORDER_OUT_FOR_DELIVERY', enabled: order },
      { key: 'PHONE_USE_SMS', enabled: useSms },
    ],
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
    await criar(config(true, true, false, true));

    expect(byId('setting-PHONE_LOGIN_CODE')!.checked).toBeFalse();
    expect(byId('setting-PHONE_ORDER_OUT_FOR_DELIVERY')!.checked).toBeTrue();
    expect(byId('setting-PHONE_USE_SMS')!.checked).toBeFalse();
    expect(fixture.nativeElement.textContent).toContain('Código de login pelo celular');
    expect(fixture.nativeElement.textContent).toContain('Enviar por SMS em vez do WhatsApp');
    expect(byId('twilio-missing')).toBeNull();
  });

  it('deveLigarEDesligar', async () => {
    await criar(config(true, true));
    service.update.and.returnValue(of({ key: 'PHONE_LOGIN_CODE', enabled: true }));

    byId('setting-PHONE_LOGIN_CODE')!.click();
    fixture.detectChanges();

    expect(service.update).toHaveBeenCalledWith('PHONE_LOGIN_CODE', true);
    expect(byId('setting-PHONE_LOGIN_CODE')!.checked).toBeTrue();
    expect(toastr.success).toHaveBeenCalled();
  });

  it('deveVoltarOInterruptorSeOBackendRecusar', async () => {
    await criar(config(true, true));
    service.update.and.returnValue(throwError(() => ({ error: { error: 'Twilio não configurado' } })));

    byId('setting-PHONE_LOGIN_CODE')!.click();
    fixture.detectChanges();

    expect(byId('setting-PHONE_LOGIN_CODE')!.checked).toBeFalse();
    expect(toastr.error).toHaveBeenCalled();
  });

  it('deveTravarSemTwilio', async () => {
    await criar(config(false, false));

    expect(byId('twilio-missing')).not.toBeNull();
    expect(byId('setting-PHONE_LOGIN_CODE')!.disabled).toBeTrue();
    expect(byId('setting-PHONE_USE_SMS')!.disabled).toBeTrue();
  });

  it('deveLiberarAsMensagensPeloCanalConfigurado', async () => {
    // só o SMS configurado: com o canal no WhatsApp, as mensagens travam, mas dá para trocar para SMS
    await criar(config(false, true));
    expect(byId('setting-PHONE_LOGIN_CODE')!.disabled).toBeTrue();
    expect(byId('setting-PHONE_USE_SMS')!.disabled).toBeFalse();
    expect(byId('twilio-missing')).toBeNull();

    fixture.destroy();
    TestBed.resetTestingModule();
    await criar(config(false, true, false, false, true));
    expect(byId('setting-PHONE_LOGIN_CODE')!.disabled).toBeFalse();
    // voltar para o WhatsApp sem ele configurado não pode
    expect(byId('setting-PHONE_USE_SMS')!.disabled).toBeTrue();
  });

  it('devePermitirDesligarOQueFicouLigado', async () => {
    await criar(config(false, false, true));

    expect(byId('setting-PHONE_LOGIN_CODE')!.disabled).toBeFalse();
  });
});
