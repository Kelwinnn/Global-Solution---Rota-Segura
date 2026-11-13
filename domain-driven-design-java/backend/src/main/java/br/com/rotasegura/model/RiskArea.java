package br.com.rotasegura.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/** Área de risco (RF01): um círculo no mapa, com centro e raio. */
@Entity
@Table(name = "T_RS_AREA")
public class RiskArea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_AREA")
    private Long id;

    @Column(name = "NAME", nullable = false, length = 100)
    private String name;

    @Column(name = "CITY", nullable = false, length = 60)
    private String city;

    @Column(name = "DISTRICT", length = 60)
    private String district;

    @Enumerated(EnumType.STRING)
    @Column(name = "RISK_TYPE", nullable = false, length = 20)
    private RiskType riskType;

    @Column(name = "LATITUDE", nullable = false, precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(name = "LONGITUDE", nullable = false, precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(name = "RADIUS_M", nullable = false)
    private Integer radiusM;

    /** Exigido pelo JPA. */
    protected RiskArea() {
    }

    public RiskArea(String name, String city, String district, RiskType riskType,
                    BigDecimal latitude, BigDecimal longitude, Integer radiusM) {
        this.name = name;
        this.city = city;
        this.district = district;
        this.riskType = riskType;
        this.latitude = latitude;
        this.longitude = longitude;
        this.radiusM = radiusM;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public RiskType getRiskType() {
        return riskType;
    }

    public void setRiskType(RiskType riskType) {
        this.riskType = riskType;
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

    public Integer getRadiusM() {
        return radiusM;
    }

    public void setRadiusM(Integer radiusM) {
        this.radiusM = radiusM;
    }
}
