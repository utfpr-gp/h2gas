import { Component, OnInit } from '@angular/core';
import { UserService } from '../../../services/user.service';
import { User } from '../../../models/user';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { NgxMaskDirective } from 'ngx-mask';
import { ToastrService } from 'ngx-toastr';
import { backendMessage } from '../../../utils/backend-message';

@Component({
  selector: 'app-customer-panel',
  imports: [
    ReactiveFormsModule,
    CommonModule,
    NgxMaskDirective
  ],
  templateUrl: './customer-panel.component.html'
})
export class CustomerPanelComponent implements OnInit {
  userLogado?: User;
  registerFormUser: FormGroup;

  constructor(
    private userService: UserService,
    private fb: FormBuilder,
    private router: Router,
    private toastr: ToastrService,
  ) {
    this.registerFormUser = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      name: ['', [Validators.required, Validators.minLength(3)]],
      phoneNumber: ['', [Validators.required]],
    });
  }

  ngOnInit(): void {
    this.userLogado = this.userService.getUserLocalStorage();
    this.registerFormUser.patchValue(this.userLogado!);
  }

  update() {
    if (this.registerFormUser.invalid) {
      this.registerFormUser.markAllAsTouched();
      this.toastr.warning('Confira os dados informados.');
      return;
    }

    const userEdit: User = this.registerFormUser.value;

    this.userService.updateUser(this.userLogado?.id!, userEdit).subscribe({
      next: (response) => {
        localStorage.setItem('user', JSON.stringify(response));
        this.toastr.success('Dados atualizados.');
      },
      error: (err) => {
        this.toastr.error(`Erro ao salvar seus dados: ${backendMessage(err)}`);
      },
    });
  }

  goToAddresses() {
    this.router.navigate(['/meus-enderecos']);
  }
}
