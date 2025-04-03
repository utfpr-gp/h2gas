export class User {
    id!: string;
    name?: string;
    email?: string;
    phoneNumber?: string;
    cpf?: string;
    cnpj?: string;
    password?: string;
    userType?: UserTypeEnum;
}

export enum UserTypeEnum {
    ADMIN = 'ADMIN',
    CLIENT = 'CLIENT',
    SUPPLIER = 'SUPPLIER'
}