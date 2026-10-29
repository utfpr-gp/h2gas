// HU25: atalho de WhatsApp (wa.me) com mensagem pronta; sem telefone válido, não há link
export function whatsappLink(phone: string | null | undefined, message: string): string | null {
  const digits = (phone ?? '').replace(/\D/g, '');
  if (digits.length < 10) return null;
  const withCountry = digits.startsWith('55') && digits.length > 11 ? digits : '55' + digits;
  return `https://wa.me/${withCountry}?text=${encodeURIComponent(message)}`;
}

// o mesmo código que a loja vê no painel
export function orderCode(id: string | null | undefined): string {
  return (id ?? '').slice(0, 8);
}

export function firstName(name: string | null | undefined): string {
  return (name ?? '').trim().split(/\s+/)[0] ?? '';
}
