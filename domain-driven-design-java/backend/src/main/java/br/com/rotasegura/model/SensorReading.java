package br.com.rotasegura.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Leitura de um sensor (RF03): chuva em mm e nível d'água em cm num instante.
 * É histórico: depois de gravada, não muda (por isso não tem setters).
 */
@Entity
@Table(name = "T_RS_SENSOR_READING")
public class SensorReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_SENSOR_READING")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ID_SENSOR", nullable = false,
            foreignKey = @ForeignKey(name = "FK_RS_READING_SENSOR"))
    private Sensor sensor;

    @Column(name = "RAIN_MM", nullable = false, precision = 6, scale = 2)
    private BigDecimal rainMm;

    @Column(name = "WATER_LEVEL_CM", nullable = false, precision = 7, scale = 2)
    private BigDecimal waterLevelCm;

    /** Instante da medição. Chega com o fuso do sensor, mas a API o converte e o grava em UTC. */
    @Column(name = "MEASURED_AT", nullable = false)
    private OffsetDateTime measuredAt;

    /** Exigido pelo JPA. */
    protected SensorReading() {
    }

    public SensorReading(Sensor sensor, BigDecimal rainMm, BigDecimal waterLevelCm, OffsetDateTime measuredAt) {
        this.sensor = sensor;
        this.rainMm = rainMm;
        this.waterLevelCm = waterLevelCm;
        this.measuredAt = measuredAt;
    }

    public Long getId() {
        return id;
    }

    public Sensor getSensor() {
        return sensor;
    }

    public BigDecimal getRainMm() {
        return rainMm;
    }

    public BigDecimal getWaterLevelCm() {
        return waterLevelCm;
    }

    public OffsetDateTime getMeasuredAt() {
        return measuredAt;
    }
}
