package br.com.rotasegura.controller;

import br.com.rotasegura.dto.CsrfTokenResponse;
import br.com.rotasegura.dto.CurrentUserResponse;
import br.com.rotasegura.dto.LoginRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Login do painel. Exceção ao fluxo controller → bo → dao: autenticação não é regra de negócio
 * (veja docs/decisions.md). O logout (POST /api/auth/logout) é feito pelo filtro do Spring Security.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação", description = "Login, sessão e token CSRF do painel")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final SecurityContextHolderStrategy securityContextHolderStrategy =
            SecurityContextHolder.getContextHolderStrategy();

    public AuthController(AuthenticationManager authenticationManager,
                          SecurityContextRepository securityContextRepository) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    @PostMapping("/login")
    @Operation(summary = "Entrar no painel",
            description = "Confere usuário e senha e abre a sessão (cookie). Responde 401 se estiverem errados.")
    public ResponseEntity<CurrentUserResponse> login(@Valid @RequestBody LoginRequest login,
                                                     HttpServletRequest request,
                                                     HttpServletResponse response) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(login.username(), login.password()));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Troca o id da sessão no login (proteção contra fixação de sessão)
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }

        SecurityContext context = securityContextHolderStrategy.createEmptyContext();
        context.setAuthentication(authentication);
        securityContextHolderStrategy.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        return ResponseEntity.ok(new CurrentUserResponse(authentication.getName()));
    }

    @GetMapping("/me")
    @Operation(summary = "Usuário logado",
            description = "Devolve o usuário da sessão atual ou 401 se não houver login.")
    public CurrentUserResponse me(Authentication authentication) {
        return new CurrentUserResponse(authentication.getName());
    }

    @GetMapping("/csrf")
    @Operation(summary = "Token CSRF",
            description = "Devolve o token e o nome do cabeçalho que o front envia em cada POST, PUT e DELETE.")
    public CsrfTokenResponse csrf(CsrfToken csrfToken) {
        return new CsrfTokenResponse(csrfToken.getHeaderName(), csrfToken.getToken());
    }
}
