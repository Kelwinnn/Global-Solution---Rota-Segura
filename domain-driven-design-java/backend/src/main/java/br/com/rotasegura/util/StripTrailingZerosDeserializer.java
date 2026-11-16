package br.com.rotasegura.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.std.NumberDeserializers;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * Lê um número do JSON como BigDecimal e tira os zeros à direita (12.400 vira 12.4).
 * Assim o @Digits só conta as casas decimais que têm valor de verdade.
 * Usado com @JsonDeserialize(using = ...) no campo do DTO.
 */
public class StripTrailingZerosDeserializer extends JsonDeserializer<BigDecimal> {

    @Override
    public BigDecimal deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        // Lê do jeito padrão do Jackson (inclusive as mesmas regras de erro) e só depois tira os zeros
        BigDecimal value = NumberDeserializers.BigDecimalDeserializer.instance.deserialize(parser, context);
        return value == null ? null : value.stripTrailingZeros();
    }
}
