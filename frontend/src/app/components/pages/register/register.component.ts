import { Component, OnInit } from '@angular/core';
import { RegisterUserComponent } from '../../register-user/register-user.component';

@Component({
  selector: 'app-register',
  imports: [
    RegisterUserComponent
  ],
  templateUrl: './register.component.html',
  styleUrl: './register.component.css'
})
export class RegisterComponent {}
