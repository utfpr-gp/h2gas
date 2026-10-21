import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { NgxMaskDirective } from 'ngx-mask';
import { ToastrService } from 'ngx-toastr';
import { UserService } from '../../../services/user.service';
import { User } from '../../../models/user';
import { backendMessage } from '../../../utils/backend-message';
import { initials } from '../../../utils/initials';

// F7 (fatia 4): "Meus Dados" de cliente, fornecedor e entregador; o e-mail é só leitura (identidade do login por código)
@Component({
  selector: 'app-customer-panel',
  imports: [ReactiveFormsModule, CommonModule, NgxMaskDirective],
  templateUrl: './customer-panel.component.html'
})
export class CustomerPanelComponent implements OnInit {
  user?: User;
  profileForm: FormGroup;
  loading = true;
  saving = false;
  private baseline = { name: '', phoneNumber: '' };

  constructor(
    private userService: UserService,
    private fb: FormBuilder,
    private router: Router,
    private toastr: ToastrService,
  ) {
    this.profileForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(3)]],
      phoneNumber: ['', [Validators.required]],
    });
  }

  ngOnInit(): void {
    this.userService.getMe().subscribe({
      next: (response: any) => this.show(response),
      error: (err) => {
        // sem a API ainda dá para mostrar o que está guardado no navegador
        this.toastr.error(`Erro ao carregar seus dados: ${backendMessage(err)}`);
        const cached = this.userService.getUserLocalStorage();
        if (cached) this.show(cached);
        this.loading = false;
      },
    });
  }

  get initials(): string {
    return initials(this.user?.name);
  }

  get userType(): string | undefined {
    return this.user?.userType;
  }

  // "Salvar" só vale com alteração real e campos válidos
  get canSave(): boolean {
    if (this.profileForm.invalid || this.saving) return false;
    const { name, phoneNumber } = this.profileForm.value;
    return name.trim() !== this.baseline.name || phoneNumber !== this.baseline.phoneNumber;
  }

  save() {
    if (!this.canSave || !this.user) {
      return;
    }
    this.saving = true;
    const { name, phoneNumber } = this.profileForm.value;
    // o e-mail vai igual ao atual: a API só aceita trocá-lo para ADMIN
    this.userService.updateUser(this.user.id!, { name: name.trim(), phoneNumber, email: this.user.email } as User).subscribe({
      next: (response: any) => {
        this.saving = false;
        this.show(response);
        this.userService.setCurrentUser(response);
        this.toastr.success('Dados atualizados.');
      },
      error: (err) => {
        this.saving = false;
        this.toastr.error(`Erro ao salvar seus dados: ${backendMessage(err)}`);
      },
    });
  }

  go(route: string) {
    this.router.navigate([route]);
  }

  private show(user: User) {
    this.user = user;
    this.baseline = { name: (user.name ?? '').trim(), phoneNumber: user.phoneNumber ?? '' };
    this.profileForm.reset({ name: user.name ?? '', phoneNumber: user.phoneNumber ?? '' });
    this.loading = false;
  }
}
