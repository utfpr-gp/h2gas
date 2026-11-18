import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastrService } from 'ngx-toastr';
import { AdminSettingsService, AppSetting, AppSettingKey } from '../../../services/admin-settings.service';
import { backendMessage } from '../../../utils/backend-message';

const LABELS: Record<AppSettingKey, { title: string; description: string; icon: string }> = {
  PHONE_LOGIN_CODE: {
    title: 'Código de login pelo celular',
    description: 'O usuário pode escolher receber o código de acesso no celular cadastrado. Sem celular, ou se o envio falhar, o código vai por e-mail.',
    icon: 'password',
  },
  PHONE_ORDER_OUT_FOR_DELIVERY: {
    title: 'Aviso de saiu para entrega',
    description: 'O cliente recebe uma mensagem quando o entregador inicia a rota do pedido.',
    icon: 'local_shipping',
  },
  PHONE_USE_SMS: {
    title: 'Enviar por SMS em vez do WhatsApp',
    description: 'Desligado, as mensagens vão pelo WhatsApp. Ligado, vão por SMS.',
    icon: 'sms',
  },
};

// HU26: o admin liga e desliga as mensagens ao celular (cada envio tem custo no Twilio) e escolhe o canal
@Component({
  selector: 'app-admin-settings',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './admin-settings.component.html',
})
export class AdminSettingsComponent implements OnInit {
  private readonly service = inject(AdminSettingsService);
  private readonly toastr = inject(ToastrService);

  readonly labels = LABELS;
  settings: AppSetting[] = [];
  whatsappConfigured = false;
  smsConfigured = false;
  loading = true;
  loadFailed = false;
  saving: AppSettingKey | null = null;

  ngOnInit(): void {
    this.load();
  }

  load() {
    this.loading = true;
    this.loadFailed = false;
    this.service.list().subscribe({
      next: (config) => {
        this.settings = config.settings;
        this.whatsappConfigured = config.whatsappConfigured;
        this.smsConfigured = config.smsConfigured;
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.loadFailed = true;
        this.toastr.error(`Não foi possível carregar as configurações: ${backendMessage(err)}`);
      },
    });
  }

  get nothingConfigured(): boolean {
    return !this.whatsappConfigured && !this.smsConfigured;
  }

  get useSms(): boolean {
    return this.settings.find((s) => s.key === 'PHONE_USE_SMS')?.enabled ?? false;
  }

  get channelLabel(): string {
    return this.useSms ? 'SMS' : 'WhatsApp';
  }

  // o mesmo que o backend aceita: desligar sempre pode; ligar ou trocar de canal exige o canal configurado
  locked(setting: AppSetting): boolean {
    if (this.saving !== null) return true;
    if (setting.key === 'PHONE_USE_SMS') {
      return setting.enabled ? !this.whatsappConfigured : !this.smsConfigured;
    }
    const configured = this.useSms ? this.smsConfigured : this.whatsappConfigured;
    return !configured && !setting.enabled;
  }

  toggle(setting: AppSetting, input: HTMLInputElement) {
    const enabled = input.checked;
    this.saving = setting.key;
    this.service.update(setting.key, enabled).subscribe({
      next: (saved) => {
        setting.enabled = saved.enabled;
        this.saving = null;
        this.toastr.success(`${LABELS[setting.key].title}: ${saved.enabled ? 'ligado' : 'desligado'}.`);
      },
      error: (err) => {
        input.checked = setting.enabled;
        this.saving = null;
        this.toastr.error(`Não foi possível salvar: ${backendMessage(err)}`);
      },
    });
  }
}
