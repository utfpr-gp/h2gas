import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AdminSettingsService } from './admin-settings.service';
import { environment } from '../../environments/environment';

// HU26
describe('AdminSettingsService', () => {
  let service: AdminSettingsService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(AdminSettingsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('deveListarEAtualizarAsConfiguracoes', () => {
    service.list().subscribe((s) => expect(s.whatsappConfigured).toBeTrue());
    httpMock.expectOne(`${environment.apiUrl}/admin/settings`).flush({ whatsappConfigured: true, smsConfigured: true, settings: [] });

    service.update('PHONE_LOGIN_CODE', true).subscribe((s) => expect(s.enabled).toBeTrue());
    const req = httpMock.expectOne(`${environment.apiUrl}/admin/settings/PHONE_LOGIN_CODE`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ enabled: true });
    req.flush({ key: 'PHONE_LOGIN_CODE', enabled: true });
  });
});
