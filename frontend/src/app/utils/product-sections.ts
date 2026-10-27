export interface ProductSectionProduct {
  type?: string;
  category?: string;
  id?: number;
}

export interface ProductCategorySection<T> {
  name: string;
  products: T[];
}

export interface ProductTypeSection<T> {
  type: string;
  categories: ProductCategorySection<T>[];
}

const OUTROS = 'Outros';

// Agrupa a página atual: tipos na ordem dos chips, categorias em ordem alfabética.
export function groupProductsByTypeAndCategory<T extends ProductSectionProduct>(
  products: T[],
  types: { name?: string }[],
): ProductTypeSection<T>[] {
  const typeOrder = types.map((t) => t.name?.trim()).filter((name): name is string => !!name);
  const byType = new Map<string, Map<string, T[]>>();

  const typeName = (raw?: string) => {
    const name = (raw ?? '').trim();
    if (!name) return OUTROS;
    return typeOrder.find((known) => known.localeCompare(name, 'pt-BR', { sensitivity: 'accent' }) === 0) ?? name;
  };

  for (const product of products) {
    const type = typeName(product.type);
    const category = (product.category ?? '').trim() || OUTROS;
    const categories = byType.get(type) ?? new Map<string, T[]>();
    const bucket = categories.get(category) ?? [];
    bucket.push(product);
    categories.set(category, bucket);
    byType.set(type, categories);
  }

  const ordered = [
    ...typeOrder.filter((type) => byType.has(type)),
    ...[...byType.keys()].filter((type) => type !== OUTROS && !typeOrder.includes(type)),
    ...(byType.has(OUTROS) ? [OUTROS] : []),
  ];

  return ordered.map((type) => ({
    type,
    categories: [...byType.get(type)!.entries()]
      .sort(([a], [b]) => a.localeCompare(b, 'pt-BR'))
      .map(([name, items]) => ({ name, products: items })),
  }));
}
