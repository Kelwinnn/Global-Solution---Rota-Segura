package br.com.rotasegura.exception;

/** O registro pedido não existe. Vira 404. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
