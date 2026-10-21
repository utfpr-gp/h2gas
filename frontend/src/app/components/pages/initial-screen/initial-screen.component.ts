import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { VisitorCepPanelComponent } from '../../visitor-cep-panel/visitor-cep-panel.component';

@Component({
  selector: 'app-initial-screen',
  imports: [VisitorCepPanelComponent],
  templateUrl: './initial-screen.component.html',
  styleUrl: './initial-screen.component.css'
})
export class InitialScreenComponent {

  constructor(
    private router: Router
  ) {}

  navigateLogin() {
    this.router.navigate(['/login']);
  }
}
