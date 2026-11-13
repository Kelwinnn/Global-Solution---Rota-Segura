package br.com.rotasegura.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/** Sensor de rua (RF02): mede chuva e nível d'água e pertence a uma área de risco. */
@Entity
@Table(name = "T_RS_SENSOR")
public class Sensor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_SENSOR")
    private Long id;

    /** Muitos sensores para uma área. LAZY: a área só é buscada no banco quando for usada. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ID_AREA", nullable = false, foreignKey = @ForeignKey(name = "FK_RS_SENSOR_AREA"))
    private RiskArea area;

    @Column(name = "CODE", nullable = false, length = 30)
    private String code;

    @Column(name = "LATITUDE", nullable = false, precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(name = "LONGITUDE", nullable = false, precision = 9, scale = 6)
    private BigDecimal longitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    private SensorStatus status;

    /** Exigido pelo JPA. */
    protected Sensor() {
    }

    public Sensor(RiskArea area, String code, BigDecimal latitude, BigDecimal longitude, SensorStatus status) {
        this.area = area;
        this.code = code;
        this.latitude = latitude;
        this.longitude = longitude;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public RiskArea getArea() {
        return area;
    }

    public void setArea(RiskArea area) {
        this.area = area;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public SensorStatus getStatus() {
        return status;
    }

    public void setStatus(SensorStatus status) {
        this.status = status;
    }
}
