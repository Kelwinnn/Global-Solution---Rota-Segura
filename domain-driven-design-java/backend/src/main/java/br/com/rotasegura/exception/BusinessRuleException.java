package br.com.rotasegura.exception;

/** Uma regra de negócio impediu a operação. Vira 409. */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
