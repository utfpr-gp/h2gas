import { Component } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-awaiting-approval',
  imports: [],
  templateUrl: './awaiting-approval.component.html',
  styleUrl: './awaiting-approval.component.css'
})
export class AwaitingApprovalComponent {

  constructor(private router: Router) {}

  goHome() {
    this.router.navigate(['/']); // ou para '/login' se preferir
  }

}
