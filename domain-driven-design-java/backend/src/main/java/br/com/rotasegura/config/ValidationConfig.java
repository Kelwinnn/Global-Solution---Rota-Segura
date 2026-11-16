package br.com.rotasegura.config;

import org.hibernate.validator.HibernateValidatorConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;

/**
 * Ajusta a validação (Bean Validation) da API inteira:
 * - as anotações de data (@PastOrPresent, @Future...) passam a usar o mesmo relógio do resto da aplicação;
 * - "no futuro" ganha uma folga de 5 minutos, para um sensor com o relógio um pouco adiantado.
 */
@Configuration
public class ValidationConfig {

    static final Duration FUTURE_TOLERANCE = Duration.ofMinutes(5);

    @Bean
    public ValidationConfigurationCustomizer clockAndTemporalTolerance(Clock clock) {
        return configuration -> {
            configuration.clockProvider(() -> clock);
            // A folga é um recurso do Hibernate Validator (a implementação usada pelo Spring Boot)
            if (configuration instanceof HibernateValidatorConfiguration hibernate) {
                hibernate.temporalValidationTolerance(FUTURE_TOLERANCE);
            }
        };
    }
}
