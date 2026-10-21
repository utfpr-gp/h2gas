export class City {
    id?: number;
    name?: string;
    state?: string;
    ibgeCode?: number;

    constructor(id: number, name: string, state: string) {
        this.id = id;
        this.name = name;
        this.state = state;
    }
}