package br.com.rotasegura.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Informe o usuário") String username,
        @NotBlank(message = "Informe a senha") String password
) {

    /** Esconde a senha se o objeto aparecer em log ou mensagem de erro. */
    @Override
    public String toString() {
        return "LoginRequest[username=" + username + ", password=***]";
    }
}
