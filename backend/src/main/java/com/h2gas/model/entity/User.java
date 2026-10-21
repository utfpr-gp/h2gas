package com.h2gas.model.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.h2gas.enums.UserTypeEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
@NoArgsConstructor
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy=GenerationType.UUID)
    private String id;

    private String name;

    private String email;

    private String phoneNumber;

    private String cpf;

//    private String cnpj;

    private String password;

    private UserTypeEnum userType;

    // conta ativa (HU12); não confundir com Supplier.active, que é a aprovação do fornecedor
    @Column(nullable = false)
    private boolean enabled = true;

    public User(String id, String name, String email, String phoneNumber,
                String cpf, String password, UserTypeEnum userType) {
        this(name, email, phoneNumber, cpf, password, userType);
        this.id = id;
    }

    public User(String name, String email, String phoneNumber,
                String cpf, String password,
                UserTypeEnum userType) {
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.cpf = cpf;
//        this.cnpj = cnpj;
        this.password = password;
        this.userType = userType;
    }


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String role = "ROLE_" + this.userType.name();
        return List.of(new SimpleGrantedAuthority(role));
//        if (this.userType == UserTypeEnum.ADMIN) {
//            return List.of(new SimpleGrantedAuthority("ROLE_ADMIN"),
//                            new SimpleGrantedAuthority("ROLE_USER"));
//        } else {
//            return List.of(new SimpleGrantedAuthority("ROLE_USER"));
//        }
    }

    @Override
    public String getUsername() {
        return name;
    }

    @Override
    public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return UserDetails.super.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return UserDetails.super.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

}
