import { Component, OnInit } from '@angular/core';
import { UserService } from '../../../services/user.service';
import {FormBuilder, FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators} from '@angular/forms';
import { ToastrService, ToastrModule } from 'ngx-toastr';
import { User } from '../../../models/user';

@Component({
  selector: 'app-register',
  imports: [
    ReactiveFormsModule,
    // ToastrModule.forRoot(),
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
      email: ['', [Validators.required, Validators.pattern('[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,4}')]],
      name: ['', Validators.required],
      phoneNumber: ['', Validators.required],
      cpf: ['', Validators.required],
      cnpj: ['', Validators.required],
      password: ['', Validators.required],
      userType: ['', Validators.required],
    })
  }

  ngOnInit(): void {
    this.user = new User('', '','', '', '', '', '', '');
  }

  register() {
    if (this.registerForm.get('password')?.value == this.confirmPassword) {
      this.user = this.registerForm?.value
      this.userService.create(this.user!!).subscribe({
        next: (response) => {
          console.log('Usuário criado com sucesso!', response);
          // this.toastr.success('Usuário criado com sucesso!');
        }, 
        error: (error) => {
          console.error('Erro ao criar usuário:', error);
          // this.toastr.error(`Erro ao criar usuário: ${error}`)
        }
      })
    }
    
  }

}
