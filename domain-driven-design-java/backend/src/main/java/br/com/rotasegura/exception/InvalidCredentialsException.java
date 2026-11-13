package br.com.rotasegura.exception;

/** Login recusado. Vira 401. O texto é sempre o mesmo, para não revelar se o usuário existe. */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Usuário ou senha incorretos.");
    }
}
