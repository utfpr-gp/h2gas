import { Component, Input, OnInit } from '@angular/core';
// import { UserService } from '../../../services/user.service';
import {FormBuilder, FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators} from '@angular/forms';
import { ToastrService, ToastrModule } from 'ngx-toastr';
// import { User } from '../../../models/user';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { User } from '../../models/user';
import { UserService } from '../../services/user.service';
import { NgxMaskDirective } from 'ngx-mask';

@Component({
  selector: 'app-register-user',
  imports: [
    ReactiveFormsModule,
    CommonModule,
    FormsModule,
    NgxMaskDirective
  ],
  templateUrl: './register-user.component.html',
  styleUrl: './register-user.component.css'
})
export class RegisterUserComponent implements OnInit {

  registerForm!: FormGroup;
  user?: User;

  confirmPassword: string =''; 

  @Input()
  userType?: string;

  @Input()
  tittleRegister?: string;


  constructor(
    private fb: FormBuilder, 
    private userService: UserService,
    private toastr: ToastrService,
    private router: Router
  ) {
    this.registerForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      name: ['', [Validators.required, Validators.minLength(3)]],
      phoneNumber: ['', [Validators.required]],
      cpf: [''],
      cnpj: [''],
      password: ['', [Validators.required, Validators.minLength(6)]],
      userType: [this.userType, [Validators.required]],
    })
  }
  // , Validators.pattern(/^\+55\s\(\d{2}\)\s\d{5}-\d{4}$/)
  ngOnInit(): void {
    this.user = new User('', '','', '', '', '', '', '');
  }

  register() {
    debugger

    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      this.toastr.warning('Conferir informações!!')
      return; 
    }


    if (this.registerForm.get('password')?.value == this.confirmPassword) {
      this.user = this.registerForm?.value
      this.userService.create(this.user!!).subscribe({
        next: (response) => {
          console.log('Usuário criado com sucesso!', response);
          this.toastr.success('Usuário criado com sucesso!');
          this.navigateRoute('/login')
        }, 
        error: (error) => {
          console.error('Erro ao criar usuário:', error);
          this.toastr.error(`Erro ao criar usuário: ${error}`)
        }
      })
    }    
  }


  isInvalid(field: string): boolean {
    return this.registerForm.controls[field].touched && this.registerForm.controls[field].invalid;
  }


  getErrorMessage(field: string): string {
    const control = this.registerForm.controls[field];

    if (control.errors?.['required']) return 'Este campo é obrigatório.';
    if (control.errors?.['minlength']) return `Mínimo ${control.errors['minlength'].requiredLength} caracteres.`;
    if (control.errors?.['email']) return 'Formato de email inválido.';
    if (control.errors?.['pattern']) return 'Formato inválido.';

    return '';
  }

  navigateRoute(route: string) {
    this.router.navigate([route])
  }

}
