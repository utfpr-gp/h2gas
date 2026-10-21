// o entregador não tem catálogo (HU07) e o fornecedor começa pelo dashboard (HU09)
export function homeFor(userType?: string): string {
  if (userType === 'DELIVERER') return '/entregas';
  return userType === 'SUPPLIER' ? '/dashboard' : '/inicio';
}
