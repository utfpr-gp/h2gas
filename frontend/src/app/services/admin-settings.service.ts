import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

// HU26: interruptores do admin (mensagens ao celular pelo Twilio, por WhatsApp ou SMS)
export type AppSettingKey = 'PHONE_LOGIN_CODE' | 'PHONE_ORDER_OUT_FOR_DELIVERY' | 'PHONE_USE_SMS';

export interface AppSetting {
  key: AppSettingKey;
  enabled: boolean;
}

export interface AppSettings {
  whatsappConfigured: boolean;
  smsConfigured: boolean;
  settings: AppSetting[];
}

@Injectable({ providedIn: 'root' })
export class AdminSettingsService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/admin/settings`;

  list(): Observable<AppSettings> {
    return this.http.get<AppSettings>(this.baseUrl);
  }

  update(key: AppSettingKey, enabled: boolean): Observable<AppSetting> {
    return this.http.put<AppSetting>(`${this.baseUrl}/${key}`, { enabled });
  }
}
