import { Component, OnInit } from '@angular/core';
import { UserService } from '../../../services/user.service';
import {FormBuilder, FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators} from '@angular/forms';
import { ToastrService, ToastrModule } from 'ngx-toastr';
import { User } from '../../../models/user';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-register',
  imports: [
    ReactiveFormsModule,
    // ToastrModule.forRoot(),
    CommonModule,
    FormsModule
  ],
  templateUrl: './register.component.html',
  styleUrl: './register.component.css'
})
export class RegisterComponent implements OnInit {

  registerForm!: FormGroup;
  user?: User;

  confirmPassword: string =''; 

  constructor(
    private fb: FormBuilder, 
    private userService: UserService,
    private toastr: ToastrService,
  ) {
    this.registerForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      name: ['', [Validators.required, Validators.minLength(3)]],
      phoneNumber: ['', [Validators.required]],
      cpf: [''],
      password: ['', [Validators.required, Validators.minLength(6)]],
      userType: ['CUSTOMER', [Validators.required]],
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
      this.toastr.success('Usuário criado com sucesso!');
      return; 
    }

    console.log("deu boaaaa");
    
    // if (this.registerForm.get('password')?.value == this.confirmPassword) {
    //   this.user = this.registerForm?.value
    //   this.userService.create(this.user!!).subscribe({
    //     next: (response) => {
    //       console.log('Usuário criado com sucesso!', response);
    //       this.toastr.success('Usuário criado com sucesso!');
    //     }, 
    //     error: (error) => {
    //       console.error('Erro ao criar usuário:', error);
    //       this.toastr.error(`Erro ao criar usuário: ${error}`)
    //     }
    //   })
    // }

    
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



}
