import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { AuthService } from '../../../services/auth.service';
import { UserService } from '../../../services/user.service';
import { LocationContextService } from '../../../services/location-context.service';
import { backendMessage } from '../../../utils/backend-message';
import { homeFor } from '../../../utils/home-route';

// HU11 (Figura 10a): entrada por código de 6 dígitos enviado por e-mail
@Component({
  selector: 'app-login-token',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './login-token.component.html',
})
export class LoginTokenComponent {
  emailForm: FormGroup;
  codeForm: FormGroup;
  step: 'email' | 'code' = 'email';
  sending = false;
  validating = false;

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private authService: AuthService,
    private userService: UserService,
    private toastr: ToastrService,
    private locationContext: LocationContextService,
  ) {
    this.emailForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
    });
    this.codeForm = this.fb.group({
      code: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
    });
  }

  get email(): string {
    return this.emailForm.value.email;
  }

  sendCode() {
    if (this.emailForm.invalid) {
      this.emailForm.markAllAsTouched();
      return;
    }
    this.sending = true;
    // avança em qualquer resposta: a tela não revela se o e-mail está cadastrado
    this.authService.requestLoginToken(this.email).subscribe({
      next: () => this.goToCodeStep(),
      error: () => this.goToCodeStep(),
    });
  }

  resendCode() {
    this.authService.requestLoginToken(this.email).subscribe({
      next: () => this.toastr.success('Enviamos um novo código.'),
      error: () => this.toastr.success('Enviamos um novo código.'),
    });
  }

  changeEmail() {
    this.step = 'email';
    this.codeForm.reset({ code: '' });
  }

  validateCode() {
    if (this.codeForm.invalid) {
      this.codeForm.markAllAsTouched();
      return;
    }
    this.validating = true;
    this.clearPreviousSession();
    this.authService.validateLoginToken(this.email, this.codeForm.value.code).subscribe({
      next: (response) => {
        this.validating = false;
        localStorage.setItem('access_token', response.token);
        this.loadMe();
      },
      error: (err) => {
        this.validating = false;
        this.toastr.error(`Não foi possível entrar: ${backendMessage(err)}`);
      },
    });
  }

  goToPasswordLogin() {
    this.router.navigate(['/login/senha']);
  }

  goToRegister() {
    this.router.navigate(['/register']);
  }

  private goToCodeStep() {
    this.sending = false;
    this.step = 'code';
  }

  // mesmo encerramento de sessão anterior usado no login por senha
  private clearPreviousSession() {
    localStorage.removeItem('access_token');
    localStorage.removeItem('user');
    this.locationContext.clear();
  }

  private loadMe() {
    this.userService.getMe().subscribe({
      next: (response: any) => {
        localStorage.setItem('user', JSON.stringify(response));
        const firstName = (response?.name || '').trim().split(/\s+/)[0];
        this.toastr.success(firstName ? `Bem-vindo(a), ${firstName}!` : 'Bem-vindo(a)!');
        this.router.navigate([homeFor(response.userType)]);
      },
      error: (err) => {
        this.toastr.error(`Erro ao carregar seus dados: ${backendMessage(err)}`);
      },
    });
  }
}
