import { Component } from '@angular/core';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { UserService } from '../../../services/user.service';
import { ToastrService } from 'ngx-toastr';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../services/auth.service';
import { LocationContextService } from '../../../services/location-context.service';
import { backendMessage } from '../../../utils/backend-message';
import { isCheckoutReturn, navigateAfterLogin, navigateKeepingReturnUrl } from '../../../utils/return-url';

@Component({
  selector: 'app-login',
  imports: [
    ReactiveFormsModule,
    CommonModule,
    FormsModule,
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {
  
  // HU13: para onde voltar depois de entrar (ex.: o checkout do visitante)
  get returnUrl(): string | null {
    return this.route.snapshot.queryParamMap.get('returnUrl');
  }

  get checkoutLogin(): boolean {
    return isCheckoutReturn(this.returnUrl);
  }

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private fb: FormBuilder, 
    private userService: UserService,
    private toastr: ToastrService,
    private authService: AuthService,
    private locationContext: LocationContextService,
  ) {
    this.loginform = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required]],
    })
  }

  loginform!: FormGroup;

  loading = false;

  login() {
    localStorage.removeItem('access_token');
    localStorage.removeItem('user');
    this.locationContext.clear();
    this.loading = true;
    this.authService.loginUser(this.loginform.value).subscribe({
      next: (response: any) => {
        this.loading = false;
        localStorage.setItem('access_token', response.token)
        this.getAndSaveMe();
      }, 
      error: (err) => {
        this.loading = false;
        console.error('Erro ao logar:', err);
        this.toastr.error(`Não foi possível entrar: ${backendMessage(err)}`)
      }
    })
  }

  getAndSaveMe() {
    this.userService.getMe().subscribe({
      next: (response: any) => {
        console.log('Usuario buscado sucesso!', response);
        localStorage.setItem('user', JSON.stringify(response))
        const firstName = (response?.name || '').trim().split(/\s+/)[0];
        this.toastr.success(firstName ? `Bem-vindo(a), ${firstName}!` : 'Bem-vindo(a)!');
        navigateAfterLogin(this.router, this.returnUrl, response.userType);
      }, 
      error: (err) => {
        console.error('Erro ao buscar o usuario logado:', err);
        this.toastr.error(`Erro ao carregar seus dados: ${backendMessage(err)}`)
      }
    })
  }

  navigate(route: string) {
    this.router.navigate([route])
  }

}
