export class ProductType {
    id?: number;
    name?: string;
    icon?: string;

    constructor(
        id?: number,
        name?: string,
        icon?: string
    ) {
        this.id = id;
        this.name = name;
        this.icon = icon;
    }
}

export const DEFAULT_PRODUCT_TYPE_ICON = 'category';

// HU12 §5.2.2: mesma lista fechada que o backend aceita
export const PRODUCT_TYPE_ICONS: { icon: string; label: string }[] = [
    { icon: 'local_fire_department', label: 'Chama' },
    { icon: 'propane_tank', label: 'Botijão' },
    { icon: 'water_drop', label: 'Gota' },
    { icon: 'local_drink', label: 'Copo' },
    { icon: 'liquor', label: 'Garrafa' },
    { icon: 'ac_unit', label: 'Gelo' },
    { icon: 'outdoor_grill', label: 'Carvão' },
    { icon: 'cleaning_services', label: 'Limpeza' },
    { icon: 'shopping_basket', label: 'Cesta' },
    { icon: 'inventory_2', label: 'Caixa' },
    { icon: DEFAULT_PRODUCT_TYPE_ICON, label: 'Genérico' },
];
