import { Component } from '@angular/core';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../../services/auth.service';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-forgot-password',
  imports: [
    ReactiveFormsModule,
    CommonModule,
    FormsModule,
  ],
  templateUrl: './forgot-password.component.html',
  styleUrl: './forgot-password.component.css'
})
export class ForgotPasswordComponent {
  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private toastr: ToastrService,
  ) {
    this.form = this.fb.group({
      email: ['', [Validators.required, Validators.email]]
    });
  }

  loading = false;
  errorMessage = '';
  form!: FormGroup;


  submit() {
    if (this.form.invalid) {
      this.toastr.warning("Por favor insira um email valido!!");
      return;
    }

    this.loading = true;
    this.errorMessage = '';
    
    
    this.authService.requestPasswordReset(this.form.value).subscribe({

      next: (response) => {
        this.loading = false;
        this.router.navigate(['/email-sent']);       
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = 'Erro ao enviar link de recuperação. Tente novamente.';   
        console.error(err);
        this.toastr.error(`Erro ao enviar link de recuperação: ${err.error.error}`) 
      },
    });
  }

  navigate(route: string) {
    this.router.navigate([route])
  }
}
