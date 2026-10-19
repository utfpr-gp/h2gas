import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ToastrService } from 'ngx-toastr';
import { DelivererService } from '../../../services/deliverer.service';
import { Deliverer } from '../../../models/deliverer';
import { fadeIn } from '../../../animations/checkout.animations';

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
        this.toastr.error(`Erro ao buscar entregadores: ${this.backendMessage(err)}`);
      },
    });
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
        this.toastr.error(`Erro ao cadastrar entregador: ${this.backendMessage(err)}`);
      },
    });
  }

  invalid(field: string): boolean {
    const control = this.form.get(field);
    return !!control && control.invalid && control.touched;
  }

  // o GlobalExceptionHandler devolve { error } para regra de negócio e { message } para os demais erros
  private backendMessage(err: any): string {
    return err?.error?.error || err?.error?.message || err?.message || 'erro inesperado';
  }
}
