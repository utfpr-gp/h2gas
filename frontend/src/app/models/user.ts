export class User {
    id?: string;
    name?: string;
    email?: string;
    phoneNumber?: string;
    cpf?: string;
    cnpj?: string;
    password?: string;
    userType?: string;
    supplierId?: number;

    constructor(id: string, name: string, email: string, phoneNumber: string, 
                cpf: string, cnpj: string, password: string, userType: string, 
                supplierId?: number) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.cpf = cpf;
        this.cnpj = cnpj;
        this.password = password;
        this.userType = userType;
        this.supplierId = supplierId;
    }
}