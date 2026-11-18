import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { SwPush } from '@angular/service-worker';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';

const DISMISSED_KEY = 'h2gas.push.dismissed';

/**
 * HU27: assinatura de notificação push deste aparelho. O SwPush só existe com o service worker
 * (build de produção); sem ele, nada é pedido e nada quebra (por isso é opcional aqui).
 */
@Injectable({ providedIn: 'root' })
export class PushService {
  private readonly http = inject(HttpClient);
  private readonly swPush = inject(SwPush, { optional: true });
  private readonly baseUrl = `${environment.apiUrl}/push`;

  get supported(): boolean {
    return !!this.swPush?.isEnabled && typeof window !== 'undefined' && 'Notification' in window;
  }

  permission(): NotificationPermission {
    return typeof Notification === 'undefined' ? 'denied' : Notification.permission;
  }

  // pergunta só a quem ainda não decidiu e não tocou em "Agora não"
  canAsk(): boolean {
    return this.supported && this.permission() === 'default' && !this.dismissed();
  }

  dismiss() {
    try { localStorage.setItem(DISMISSED_KEY, '1'); } catch { /* sem storage: volta a aparecer, sem prejuízo */ }
  }

  // pede a permissão (o navegador mostra o aviso dele) e grava a assinatura para o usuário logado
  async enable(): Promise<boolean> {
    if (!this.supported) return false;
    const key = await firstValueFrom(this.http.get<{ publicKey: string; configured: boolean }>(`${this.baseUrl}/public-key`));
    if (!key.configured || !key.publicKey) return false;
    const subscription = await this.swPush!.requestSubscription({ serverPublicKey: key.publicKey });
    await firstValueFrom(this.http.post<void>(`${this.baseUrl}/subscriptions`, subscription.toJSON()));
    return true;
  }

  // permissão já dada (ex.: outra conta entrou neste aparelho): grava a assinatura de novo, sem perguntar
  async syncIfGranted(): Promise<void> {
    if (!this.supported || this.permission() !== 'granted') return;
    try {
      await this.enable();
    } catch {
      // sem push não é erro para o usuário: o app segue funcionando
    }
  }

  // ao sair: apaga no backend e no navegador. O token é lido na hora da chamada, porque o logout
  // o apaga logo em seguida, antes de a assinatura do navegador ser lida
  async disable(): Promise<void> {
    if (!this.swPush?.isEnabled) return;
    const token = localStorage.getItem('access_token');
    const subscription = await firstValueFrom(this.swPush.subscription);
    if (!subscription) return;
    try {
      const headers = token ? new HttpHeaders({ Authorization: `Bearer ${token}` }) : undefined;
      await firstValueFrom(this.http.delete<void>(`${this.baseUrl}/subscriptions`, { headers, body: { endpoint: subscription.endpoint } }));
    } catch {
      // segue para apagar no navegador mesmo se o backend não responder
    }
    await this.swPush.unsubscribe().catch(() => undefined);
  }

  private dismissed(): boolean {
    try { return localStorage.getItem(DISMISSED_KEY) === '1'; } catch { return false; }
  }
}
