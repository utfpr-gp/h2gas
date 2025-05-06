import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-dynamic-table',
  imports: [
    CommonModule,
    FormsModule
  ],
  templateUrl: './dynamic-table.component.html',
  styleUrl: './dynamic-table.component.css'
})
export class DynamicTableComponent {
  @Input() data: any[] = [];
  
  @Input() columns: { 
    key: string, 
    label: string 
  }[] = [];
  
  
  @Input() actions: {
    label: string,
    icon?: string,
    class?: string,
    onClick: (row: any) => void
  }[] = [];

  getNestedValue(obj: any, path: string): any {
    return path.split('.').reduce((acc, key) => acc?.[key], obj);
  }
}
