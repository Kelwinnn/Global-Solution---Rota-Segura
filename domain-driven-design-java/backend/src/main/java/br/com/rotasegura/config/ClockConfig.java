package br.com.rotasegura.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * O relógio da aplicação vira um bean: o código pede o Clock em vez de chamar a hora do sistema direto.
 * Nos testes, um Clock fixo deixa "agora" sempre igual.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
