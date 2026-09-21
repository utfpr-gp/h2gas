package com.h2gas.configuration.security;

import com.h2gas.enums.UserTypeEnum;
import com.h2gas.model.entity.User;
import com.h2gas.model.repository.UserRepository;
import com.h2gas.services.auth.AuthorizationService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityFilterTest {

    @Mock
    private TokenService tokenService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private SecurityFilter securityFilter;

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Deve aceitar token via query param apenas para rota SSE")
    void deveAceitarTokenViaQueryParamApenasParaRotaSse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/sse/subscribe/order:123");
        request.setRequestURI("/h2gas/api/sse/subscribe/order:123");
        request.setParameter("token", "jwt-sse");
        MockHttpServletResponse response = new MockHttpServletResponse();

        User user = new User("Cliente", "cliente@h2gas.test", "41999999999", "1", "senha", UserTypeEnum.CUSTOMER);
        when(tokenService.validateToken("jwt-sse")).thenReturn("cliente@h2gas.test");
        when(authorizationService.loadUserByUsername("cliente@h2gas.test")).thenReturn(user);

        securityFilter.doFilter(request, response, filterChain);

        verify(tokenService).validateToken("jwt-sse");
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Deve rejeitar query param token para rota que não é SSE")
    void deveRejeitarQueryParamParaRotaQueNaoESse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
        request.setRequestURI("/h2gas/api/orders");
        request.setParameter("token", "jwt-vazado");
        MockHttpServletResponse response = new MockHttpServletResponse();

        securityFilter.doFilter(request, response, filterChain);

        verify(tokenService, never()).validateToken(anyString());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }
}
