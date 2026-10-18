// rótulos de exibição das formas de pagamento gravadas no pedido
export function paymentLabel(method?: string): string {
  switch (method) {
    case 'PIX': return 'PIX';
    case 'DINHEIRO': return 'Dinheiro';
    case 'CREDIT_CARD': return 'Cartão de crédito';
    default: return method || 'Não informado';
  }
}
