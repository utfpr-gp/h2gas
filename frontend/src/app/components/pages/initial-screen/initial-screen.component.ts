import { Component } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-initial-screen',
  imports: [],
  templateUrl: './initial-screen.component.html',
  styleUrl: './initial-screen.component.css'
})
export class InitialScreenComponent {

  constructor(
    private router: Router
  ) {

  }

  navigateLogin() {
    this.router.navigate(['/login'])
  }

}
