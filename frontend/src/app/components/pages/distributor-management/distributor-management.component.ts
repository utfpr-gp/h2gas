import { Component } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-distributor-management',
  imports: [],
  templateUrl: './distributor-management.component.html',
  styleUrl: './distributor-management.component.css'
})
export class DistributorManagementComponent {
  
  constructor(
    private router: Router,
  ) {

  }


  navigate(route: string) {
    this.router.navigate([route])
  }

}
