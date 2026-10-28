import { Component, EventEmitter, OnInit, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { ToastrService } from 'ngx-toastr';
import { SupplierService } from '../../../services/supplier.service';
import { SupplierSettings } from '../../../models/supplier-settings';
import { backendMessage } from '../../../utils/backend-message';

const TIME = /^([01]\d|2[0-3]):[0-5]\d$/;

const sameTimeValidator = (group: AbstractControl): ValidationErrors | null => {
  const { openingTime, closingTime } = group.value;
  return openingTime && openingTime === closingTime ? { sameTime: true } : null;
};

type Section = 'hours' | 'stock';
const SECTION_FIELDS: Record<Section, (keyof SupplierSettings)[]> = {
  hours: ['openingTime', 'closingTime', 'averageDeliveryTime'],
  stock: ['lowStockThreshold'],
};
const SAVED_MESSAGE: Record<Section, string> = {
  hours: 'Horário e entrega salvos.',
  stock: 'Estoque mínimo salvo.',
};

// F7 fatia 6 + F9: seções "Funcionamento e entrega" e "Estoque" da tela da loja; cada uma salva sozinha
@Component({
  selector: 'app-configuration',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './configuration.component.html',
  host: { class: 'block' },
})
export class ConfigurationComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  readonly form = this.fb.nonNullable.group({
    openingTime: ['', [Validators.required, Validators.pattern(TIME)]],
    closingTime: ['', [Validators.required, Validators.pattern(TIME)]],
    averageDeliveryTime: [30, [Validators.required, Validators.min(1), Validators.max(240)]],
    lowStockThreshold: [8, [Validators.required, Validators.min(1), Validators.max(9999)]],
  }, { validators: sameTimeValidator });

  // F9: a tela da loja rola até a seção do #fragmento depois que isto carrega
  @Output() loaded = new EventEmitter<void>();

  loading = true;
  loadFailed = false;
  saving: Section | null = null;
  private saved?: SupplierSettings;

  constructor(
    private supplierService: SupplierService,
    private toastr: ToastrService,
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load() {
    this.loading = true;
    this.loadFailed = false;
    this.supplierService.getMySettings().subscribe({
      next: (settings) => {
        this.applySaved(settings);
        this.loading = false;
        this.loaded.emit();
      },
      error: (err) => {
        this.loading = false;
        this.loadFailed = true;
        this.toastr.error(`Erro ao carregar as configurações: ${backendMessage(err)}`);
      },
    });
  }

  changed(section: Section): boolean {
    const v = this.value();
    return !!this.saved && SECTION_FIELDS[section].some((k) => v[k] !== this.saved![k]);
  }

  private sectionValid(section: Section): boolean {
    const fieldsOk = SECTION_FIELDS[section].every((k) => this.form.controls[k].valid);
    return section === 'hours' ? fieldsOk && !this.sameTime : fieldsOk;
  }

  canSave(section: Section): boolean {
    return this.changed(section) && this.sectionValid(section) && this.saving === null;
  }

  // a loja que fecha antes de abrir funciona até a madrugada
  get preview(): string {
    const { openingTime, closingTime, averageDeliveryTime } = this.form.getRawValue();
    if (!openingTime || !closingTime || this.form.hasError('sameTime')) return '';
    const nextDay = closingTime < openingTime ? ' do dia seguinte' : '';
    const delivery = this.form.controls.averageDeliveryTime.valid ? ` · entrega em ~${averageDeliveryTime} min` : '';
    return `Aberta das ${openingTime} às ${closingTime}${nextDay}${delivery}`;
  }

  invalid(control: keyof typeof this.form.controls): boolean {
    const c = this.form.controls[control];
    return c.invalid && (c.touched || c.dirty);
  }

  get sameTime(): boolean {
    return this.form.hasError('sameTime');
  }

  // envia os valores salvos das outras seções e só os campos desta (F9 Cenário 6)
  save(section: Section) {
    if (!this.canSave(section)) {
      SECTION_FIELDS[section].forEach((k) => this.form.controls[k].markAsTouched());
      return;
    }
    const current = this.value();
    const request: SupplierSettings = { ...this.saved! };
    for (const k of SECTION_FIELDS[section]) (request as any)[k] = current[k];
    this.saving = section;
    this.supplierService.updateMySettings(request).subscribe({
      next: (settings) => {
        this.saving = null;
        this.applySaved(settings, section);
        this.toastr.success(SAVED_MESSAGE[section]);
      },
      error: (err) => {
        this.saving = null;
        this.toastr.error(`Não foi possível salvar: ${backendMessage(err)}`);
      },
    });
  }

  private value(): SupplierSettings {
    const v = this.form.getRawValue();
    return {
      openingTime: v.openingTime,
      closingTime: v.closingTime,
      averageDeliveryTime: Number(v.averageDeliveryTime),
      lowStockThreshold: Number(v.lowStockThreshold),
    };
  }

  // depois de salvar uma seção, só os campos dela voltam ao valor salvo; a edição pendente da outra fica
  private applySaved(settings: SupplierSettings, section?: Section) {
    this.saved = { ...settings, openingTime: settings.openingTime?.slice(0, 5), closingTime: settings.closingTime?.slice(0, 5) };
    if (!section) {
      this.form.reset(this.saved);
      return;
    }
    for (const k of SECTION_FIELDS[section]) {
      this.form.controls[k].reset(this.saved[k] as never);
    }
  }
}
