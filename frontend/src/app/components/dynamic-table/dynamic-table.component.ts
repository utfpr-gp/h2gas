import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
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

  @Output() switchChanged = new EventEmitter<{ row: any; key: string; value: boolean }>();

  
  @Input() columns: { 
    key: string, 
    label: string,
    type?: string
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

  onSwitchToggle(row: any, key: string, value: any): void {
    // Atualiza o valor na linha
    debugger
    const checked = value.target.checked;
    this.switchChanged.emit({ row, key, value: checked });
    // const keys = key.split('.');
    // let target = row;
    // for (let i = 0; i < keys.length - 1; i++) {
    //   target = target[keys[i]];
    // }
    // target[keys[keys.length - 1]] = value.target.checked;
  
    // // // Opcional: emita um evento, salve no backend, etc.
    // console.log('Switch changed:', key, value.target.checked);
  }

  
}
