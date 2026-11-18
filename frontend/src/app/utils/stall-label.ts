// HU28: tempo de pedido parado, vindo do backend em minutos
export function stallLabel(minutes: number): string {
  if (minutes < 60) return `parado há ${minutes} min`;
  if (minutes < 1440) return `parado há ${Math.floor(minutes / 60)} h`;
  const days = Math.floor(minutes / 1440);
  return `parado há ${days} ${days === 1 ? 'dia' : 'dias'}`;
}
