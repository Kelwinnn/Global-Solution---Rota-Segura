package br.com.rotasegura.dto;

/** Um campo com erro de validação, dentro do "errors" do ProblemDetail. */
public record InvalidFieldResponse(String field, String message) {
}
