import { Component } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-email-sent',
  imports: [],
  templateUrl: './email-sent.component.html',
  styleUrl: './email-sent.component.css'
})
export class EmailSentComponent {
  constructor(
    private router: Router
  ) {}


  navigate(route: string) {
    this.router.navigate([route])
  }
}
