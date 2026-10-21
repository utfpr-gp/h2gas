import { Component, OnInit, inject } from '@angular/core';
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

// F7 fatia 6: configurações da loja do fornecedor logado (horário, entrega e estoque mínimo)
@Component({
  selector: 'app-configuration',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './configuration.component.html',
  host: { class: 'flex-1 flex flex-col' },
})
export class ConfigurationComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  readonly form = this.fb.nonNullable.group({
    openingTime: ['', [Validators.required, Validators.pattern(TIME)]],
    closingTime: ['', [Validators.required, Validators.pattern(TIME)]],
    averageDeliveryTime: [30, [Validators.required, Validators.min(1), Validators.max(240)]],
    lowStockThreshold: [8, [Validators.required, Validators.min(1), Validators.max(9999)]],
  }, { validators: sameTimeValidator });

  loading = true;
  loadFailed = false;
  saving = false;
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
      },
      error: (err) => {
        this.loading = false;
        this.loadFailed = true;
        this.toastr.error(`Erro ao carregar as configurações: ${backendMessage(err)}`);
      },
    });
  }

  get changed(): boolean {
    const v = this.value();
    return !!this.saved && (Object.keys(v) as (keyof SupplierSettings)[]).some((k) => v[k] !== this.saved![k]);
  }

  get canSave(): boolean {
    return this.changed && this.form.valid && !this.saving;
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

  save() {
    if (!this.canSave) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving = true;
    this.supplierService.updateMySettings(this.value()).subscribe({
      next: (settings) => {
        this.saving = false;
        this.applySaved(settings);
        this.toastr.success('Configurações salvas.');
      },
      error: (err) => {
        this.saving = false;
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

  private applySaved(settings: SupplierSettings) {
    this.saved = { ...settings, openingTime: settings.openingTime?.slice(0, 5), closingTime: settings.closingTime?.slice(0, 5) };
    this.form.reset(this.saved);
  }
}
