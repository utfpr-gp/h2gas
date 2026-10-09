import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

interface TabBarItem {
  id: string;
  label: string;
  icon: string;
  path: string;
  queryParams: Record<string, string>;
}

@Component({
  selector: 'app-bottom-tab-bar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './bottom-tab-bar.component.html'
})
export class BottomTabBarComponent {
  readonly tabs: TabBarItem[] = [
    { id: 'inicio', label: 'Início', icon: 'home', path: '/inicio', queryParams: {} },
    { id: 'categorias', label: 'Categorias', icon: 'category', path: '/inicio', queryParams: { view: 'products' } },
    { id: 'pedidos', label: 'Pedidos', icon: 'shopping_bag', path: '/meus-pedidos', queryParams: {} },
    { id: 'perfil', label: 'Perfil', icon: 'person', path: '/painel-cliente', queryParams: {} },
  ];

  constructor(private router: Router) {}

  navigateTo(tab: TabBarItem) {
    this.router.navigate([tab.path], { queryParams: tab.queryParams });
  }
}
