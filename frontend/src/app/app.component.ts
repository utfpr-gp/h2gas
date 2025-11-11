import { Component } from '@angular/core';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { FooterComponent } from './components/footer/footer.component';
import { MenuComponent } from './components/menu/menu.component';
import { filter } from 'rxjs/operators';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-root',
  imports: [
    RouterOutlet,
    FooterComponent,
    MenuComponent,
    CommonModule,
  ],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
  title = 'frontend';
  showMenu = true;
  hiddenMenuRoutes = [
    '/',
    '/login', 
    '/register', 
    '/register-supplier', 
    '/unauthorized', 
    '/aguardando-aprovacao',
    '/distributor/new',
    '/recuperar-senha',
    '/checagem-email',
    '/resetar-senha',
    '/email-sent',
    '/contato',
    '/sobre'
  ];


  constructor(private router: Router) {
    this.router.events
    .pipe(filter(event => event instanceof NavigationEnd))
    .subscribe((event: NavigationEnd) => {
      const cleanUrl = event.urlAfterRedirects.split('?')[0];
      this.showMenu = !this.hiddenMenuRoutes.includes(cleanUrl);
    });
  }
}
