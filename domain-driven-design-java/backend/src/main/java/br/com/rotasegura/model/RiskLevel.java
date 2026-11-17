package br.com.rotasegura.model;

/**
 * Nível de risco da área pelo nível d'água em cm (RN01), do menos ao mais urgente.
 * A ordem importa: quanto maior o ordinal, mais urgente ir para um abrigo ou local mais alto.
 * As faixas ficam no RiskLevelBO (regra de negócio).
 */
public enum RiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    VERY_HIGH,
    VERY_URGENT
}
