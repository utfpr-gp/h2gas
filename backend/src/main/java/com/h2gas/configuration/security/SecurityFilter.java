package com.h2gas.configuration.security;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.h2gas.model.repository.UserRepository;
import com.h2gas.services.auth.AuthorizationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class SecurityFilter extends OncePerRequestFilter {

    @Autowired
    TokenService tokenService;

    @Autowired
    UserRepository userRepository;
    @Autowired
    private AuthorizationService authorizationService;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
       try {
           var token = this.recoverToken(request);

           if (token != null) {
               var subject = tokenService.validateToken(token);
               UserDetails user = authorizationService.loadUserByUsername(subject);

               var authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
               SecurityContextHolder.getContext().setAuthentication(authentication);
           }

           filterChain.doFilter(request, response);
       } catch (TokenExpiredException e) {
           response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
           response.getWriter().write("Token expirado");
           response.getWriter().flush();
       } catch (JWTVerificationException e) {
           response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
           response.getWriter().write("Token inválido");
           response.getWriter().flush();
       }

    }

    private String recoverToken(HttpServletRequest request) {
        var authHeader = request.getHeader("Authorization");
        if (authHeader != null) {
            return authHeader.replace("Bearer ", "");
        }
        // EventSource nativo não envia headers customizados — fallback por query param,
        // restrito às rotas de SSE.
        if (request.getRequestURI().startsWith("/h2gas/api/sse/")) {
            return request.getParameter("token");
        }
        return null;
    }
}
