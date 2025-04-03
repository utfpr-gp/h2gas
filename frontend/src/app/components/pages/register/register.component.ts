import { Component } from '@angular/core';
import { UserService } from '../../../services/user.service';
import {FormBuilder, FormControl, ReactiveFormsModule} from '@angular/forms';
// import { ToastrService, ToastrModule } from 'ngx-toastr';

@Component({
  selector: 'app-register',
  imports: [
    ReactiveFormsModule,
    // ToastrModule
  ],
  templateUrl: './register.component.html',
  styleUrl: './register.component.css'
})
export class RegisterComponent {

  constructor(
    private fb: FormBuilder, 
    private userService: UserService,
    // private toastr: ToastrService,
  ) {}


  register() {

  }

}
