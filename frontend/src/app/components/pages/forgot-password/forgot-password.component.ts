import { Component } from '@angular/core';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../../services/auth.service';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';

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
    private router: Router
  ) {
    this.form = this.fb.group({
      email: ['', [Validators.required, Validators.email]]
    });
  }

  loading = false;
  errorMessage = '';
  form!: FormGroup;


  submit() {
    if (this.form.invalid) return;

    this.loading = true;
    this.errorMessage = '';

    // this.authService.requestPasswordReset(this.form.value.email!).subscribe({
    //   next: () => {
    //     this.loading = false;
    //     this.router.navigate(['/check-email']);
    //   },
    //   error: () => {
    //     this.loading = false;
    //     this.errorMessage = 'Erro ao enviar link de recuperação. Tente novamente.';
    //   }
    // });
  }
}
