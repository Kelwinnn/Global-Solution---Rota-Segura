package br.com.rotasegura.model;

/** Por que a pessoa assistida precisa de ajuda para sair de casa numa emergência. */
public enum SpecialNeed {
    /** Pessoa idosa. */
    ELDERLY,
    /** Pessoa acamada. */
    BEDRIDDEN,
    /** Usuária de cadeira de rodas. */
    WHEELCHAIR,
    /** Outro motivo (detalhado em notes). */
    OTHER
}
