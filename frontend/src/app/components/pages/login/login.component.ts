import { Component } from '@angular/core';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { UserService } from '../../../services/user.service';
import { ToastrService } from 'ngx-toastr';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../services/auth.service';
import { CartService } from '../../../services/cart.service';

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
  
  constructor(
    private router: Router,
    private fb: FormBuilder, 
    private userService: UserService,
    private toastr: ToastrService,
    private authService: AuthService,
    private cartService: CartService,
  ) {
    this.loginform = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required]],
    })
  }

  loginform!: FormGroup;


  login() {
    localStorage.removeItem('access_token');
    localStorage.removeItem('user');
    this.cartService.setCart([]);
    this.authService.loginUser(this.loginform.value).subscribe({
      next: (response: any) => {
        console.log('Usuario logado com sucesso!', response);
        this.toastr.success('Usuario logado com sucesso!');
        localStorage.setItem('access_token', response.token)
        this.getAndSaveMe();
      }, 
      error: (err) => {
        console.error('Erro ao logar:', err);
        this.toastr.error(`Erro ao efetuar o login: ${err.error.message}`)
      }
    })
  }

  getAndSaveMe() {
    this.userService.getMe().subscribe({
      next: (response: any) => {
        console.log('Usuario buscado sucesso!', response);
        localStorage.setItem('user', JSON.stringify(response))
        this.navigate('/inicio')
        // if (response.userType == "SUPPLIER" && response.supplierId == null) {
        //   this.navigate('/distributor/new') 
        // } else {
        //   this.navigate('/inicio')
        // }        
      }, 
      error: (err) => {
        console.error('Erro ao buscar o usuario logado:', err);
        this.toastr.error(`Erro ao buscar o usuario: ${err.error.message}`)
      }
    })
  }

  navigate(route: string) {
    this.router.navigate([route])
  }

}
