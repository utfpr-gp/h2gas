package com.h2gas.controller;

import com.h2gas.configuration.security.TokenService;
import com.h2gas.enums.UserTypeEnum;
import com.h2gas.exceptions.BusinessRuleException;
import com.h2gas.model.dto.*;
import com.h2gas.model.entity.Supplier;
import com.h2gas.model.entity.User;
import com.h2gas.model.repository.UserRepository;
import com.h2gas.services.LoginTokenService;
import com.h2gas.services.SupplierService;
import com.h2gas.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Autenticação")
@RestController
@RequestMapping("api/auth")
public class AuthenticationController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UserService userService;

    @Autowired
    private LoginTokenService loginTokenService;

    @Operation(summary = "Pede um código de acesso por e-mail (público)",
            description = "Gera um código numérico de 6 dígitos, válido por 5 minutos, e envia por e-mail. "
                    + "Responde 200 mesmo quando o e-mail não está cadastrado, para não revelar a existência da conta; "
                    + "pedir de novo substitui o código anterior")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedido recebido (o e-mail só é enviado se a conta existir)"),
            @ApiResponse(responseCode = "400", description = "E-mail ausente ou inválido")
    })
    @PostMapping("/login/token/request")
    public ResponseEntity<Void> requestLoginToken(@RequestBody @Valid RequestLoginTokenDTO data) {
        loginTokenService.requestToken(data.getEmail());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Entra com o código recebido por e-mail (público)",
            description = "Valida o código de 6 dígitos e devolve o mesmo JWT Bearer do login por senha. "
                    + "Código errado conta uma tentativa (máximo de 5); expirado, usado ou esgotado precisa de um novo pedido")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Código válido; devolve o token"),
            @ApiResponse(responseCode = "400", description = "Código inválido, expirado ou com tentativas esgotadas; ou formato inválido")
    })
    @PostMapping("/login/token/validate")
    public ResponseEntity<LoginDTO> validateLoginToken(@RequestBody @Valid ValidateLoginTokenDTO data) {
        String token = loginTokenService.validateAndLogin(data.getEmail(), data.getCode());
        return ResponseEntity.ok(new LoginDTO(token));
    }

    @Operation(summary = "Login (público)", description = "Troca e-mail e senha por um JWT Bearer")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login feito; devolve o token"),
            @ApiResponse(responseCode = "401", description = "E-mail ou senha inválidos")
    })
    @PostMapping("/login")
    public ResponseEntity<LoginDTO> login(@RequestBody @Validated AuthenticationDTO data) {
        var userNamePassword = new UsernamePasswordAuthenticationToken(data.email(), data.password());
        var auth = this.authenticationManager.authenticate(userNamePassword);

        var token = tokenService.generateToken((User) auth.getPrincipal());
        return ResponseEntity.ok(new LoginDTO(token));
    }

    @PostMapping("/register")
    public ResponseEntity<UserDto> register(@RequestBody @Validated RegisterDTO data) {
        System.out.println(this.userRepository.findByEmail(data.getEmail()));
        UserDto registerDTO = userService.create(data);
        return ResponseEntity.ok(registerDTO);
    }

}
