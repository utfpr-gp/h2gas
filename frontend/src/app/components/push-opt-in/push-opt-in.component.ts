import { Component, Input, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastrService } from 'ngx-toastr';
import { PushService } from '../../services/push.service';

type Audience = 'customer' | 'supplier' | 'deliverer';

const REASON: Record<Audience, string> = {
  customer: 'Saiba na hora quando o pedido for confirmado, sair para entrega e chegar, mesmo com o app fechado.',
  supplier: 'Saiba na hora quando entrar um pedido novo, mesmo com o app fechado.',
  deliverer: 'Saiba na hora quando receber uma nova entrega, mesmo com o app fechado.',
};

// HU27: convite para ativar as notificações push; some depois de ativar, negar ou tocar em "Agora não"
@Component({
  selector: 'app-push-opt-in',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './push-opt-in.component.html',
})
export class PushOptInComponent implements OnInit {
  private readonly push = inject(PushService);
  private readonly toastr = inject(ToastrService);

  @Input({ required: true }) audience!: Audience;

  visible = false;
  enabling = false;

  get reason(): string {
    return REASON[this.audience];
  }

  ngOnInit(): void {
    this.visible = this.push.canAsk();
    // quem já deu a permissão (ex.: entrou com outra conta neste aparelho) volta a receber sem perguntar
    void this.push.syncIfGranted();
  }

  async enable() {
    this.enabling = true;
    try {
      const ok = await this.push.enable();
      if (ok) {
        this.toastr.success('Notificações ativadas neste aparelho.');
      } else {
        this.toastr.warning('As notificações não foram ativadas.');
      }
      this.visible = !ok && this.push.canAsk();
    } catch {
      this.toastr.warning('As notificações não foram ativadas. Confira a permissão do navegador.');
      this.visible = this.push.canAsk();
    } finally {
      this.enabling = false;
    }
  }

  dismiss() {
    this.push.dismiss();
    this.visible = false;
  }
}
