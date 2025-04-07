import { Component } from '@angular/core';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { UserService } from '../../../services/user.service';
import { ToastrService } from 'ngx-toastr';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../services/auth.service';

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
    private authService: AuthService
  ) {
    this.loginform = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required]],
    })
  }

  loginform!: FormGroup;


  login() {
    debugger
    this.authService.loginUser(this.loginform.value).subscribe({
      next: (response: any) => {
        console.log('Usuario logado com sucesso!', response);
        this.toastr.success('Usuario logado com sucesso!');
        localStorage.setItem('access_token', response.token)
        // this.navigateRoute('/login')
      }, 
      error: (error) => {
        console.error('Erro ao logar:', error);
        this.toastr.error(`Erro ao efetuar o login: ${error}`)
      }
    })
  }

  navigate(route: string) {
    console.log("funcionou");
    console.log(route);
    
    this.router.navigate([route])
  }

}
