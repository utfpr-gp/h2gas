export function homeFor(userType?: string): string {
  if (userType === 'DELIVERER') return '/entregas';
  if (userType === 'ADMIN') return '/admin';
  return userType === 'SUPPLIER' ? '/dashboard' : '/inicio';
}
