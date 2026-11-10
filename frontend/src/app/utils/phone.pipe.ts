import { Pipe, PipeTransform } from '@angular/core';

// HU28: telefone legível — (42) 99000-0011 (celular) ou (42) 3622-0000 (fixo).
// O 55 só é tratado como DDI com mais de 11 dígitos (DDD 55 existe; mesma regra do whatsapp-link).
@Pipe({ name: 'phone', standalone: true })
export class PhonePipe implements PipeTransform {
  transform(value: string | null | undefined): string {
    if (!value) return '';
    let digits = value.replace(/\D/g, '');
    if (digits.startsWith('55') && digits.length > 11) digits = digits.slice(2);
    if (digits.length === 11) return `(${digits.slice(0, 2)}) ${digits.slice(2, 7)}-${digits.slice(7)}`;
    if (digits.length === 10) return `(${digits.slice(0, 2)}) ${digits.slice(2, 6)}-${digits.slice(6)}`;
    return value;
  }
}
