export class User {
    id: string;
    name?: string;
    email?: string;
    phoneNumber?: string;
    cpf?: string;
    cnpj?: string;
    password?: string;
    userType?: string;
    supplierId?: string;

    constructor(id: string, name: string, email: string, phoneNumber: string, 
                cpf: string, cnpj: string, password: string, userType: string, 
                supplierId?: string) {
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