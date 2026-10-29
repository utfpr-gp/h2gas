import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ToastrService } from 'ngx-toastr';
import { DelivererService } from '../../../services/deliverer.service';
import { Deliverer } from '../../../models/deliverer';
import { fadeIn } from '../../../animations/checkout.animations';
import { backendMessage } from '../../../utils/backend-message';

// HU07: o fornecedor cadastra e acompanha a própria frota
@Component({
  selector: 'app-manage-deliverers',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './manage-deliverers.component.html',
  animations: [fadeIn],
})
export class ManageDeliverersComponent implements OnInit {
  deliverers: Deliverer[] = [];
  loading = true;
  saving = false;

  form: FormGroup;

  constructor(
    fb: FormBuilder,
    private delivererService: DelivererService,
    private toastr: ToastrService,
  ) {
    this.form = fb.group({
      name: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      phoneNumber: ['', Validators.required],
      password: ['', [Validators.required, Validators.minLength(6)]],
      vehicleInfo: [''],
    });
  }

  ngOnInit(): void {
    this.load();
  }

  load() {
    this.delivererService.listMine().subscribe({
      next: (frota) => {
        this.deliverers = frota;
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.toastr.error(`Erro ao buscar entregadores: ${backendMessage(err)}`);
      },
    });
  }

  // ---- HU22: e-mail copiável e senha redefinida pelo fornecedor ----
  expandedId: number | null = null;
  resettingId: number | null = null;

  toggleDetails(deliverer: Deliverer) {
    this.expandedId = this.expandedId === deliverer.id ? null : deliverer.id;
    this.resettingId = null;
    this.newPassword = this.confirmPassword = '';
  }
  savingPassword = false;
  newPassword = '';
  confirmPassword = '';
  copiedId: number | null = null;

  openReset(deliverer: Deliverer) {
    this.resettingId = deliverer.id;
    this.newPassword = '';
    this.confirmPassword = '';
  }

  get passwordMismatch(): boolean {
    return !!this.confirmPassword && this.newPassword !== this.confirmPassword;
  }

  get canSavePassword(): boolean {
    return this.newPassword.length >= 6 && this.newPassword === this.confirmPassword && !this.savingPassword;
  }

  savePassword(deliverer: Deliverer) {
    if (!this.canSavePassword) return;
    this.savingPassword = true;
    this.delivererService.resetPassword(deliverer.id, this.newPassword).subscribe({
      next: () => {
        this.savingPassword = false;
        this.resettingId = null;
        this.newPassword = this.confirmPassword = '';
        this.toastr.success(`Senha de ${deliverer.name} redefinida.`);
      },
      error: (err) => {
        this.savingPassword = false;
        this.toastr.error(`Não foi possível redefinir a senha: ${backendMessage(err)}`);
      },
    });
  }

  copyEmail(deliverer: Deliverer) {
    const done = () => {
      this.copiedId = deliverer.id;
      setTimeout(() => (this.copiedId = null), 1500);
    };
    try {
      navigator.clipboard.writeText(deliverer.email).then(done, () => this.toastr.warning('Não foi possível copiar; selecione o e-mail.'));
    } catch {
      this.toastr.warning('Não foi possível copiar; selecione o e-mail.');
    }
  }

  save() {
    if (this.form.invalid || this.saving) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving = true;
    const value = this.form.getRawValue();
    this.delivererService.create({
      name: value.name,
      email: value.email,
      phoneNumber: value.phoneNumber,
      password: value.password,
      vehicleInfo: value.vehicleInfo || undefined,
    }).subscribe({
      next: (criado) => {
        this.saving = false;
        this.toastr.success(`${criado.name} foi adicionado à frota.`);
        this.form.reset();
        this.load();
      },
      error: (err) => {
        this.saving = false;
        this.toastr.error(`Erro ao cadastrar entregador: ${backendMessage(err)}`);
      },
    });
  }

  invalid(field: string): boolean {
    const control = this.form.get(field);
    return !!control && control.invalid && control.touched;
  }
}
