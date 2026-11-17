package br.com.rotasegura.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/** Abrigo (RF06): local seguro com endereço, posição no mapa e capacidade. */
@Entity
@Table(name = "T_RS_SHELTER")
public class Shelter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_SHELTER")
    private Long id;

    @Column(name = "NAME", nullable = false, length = 150)
    private String name;

    @Column(name = "ADDRESS", nullable = false, length = 200)
    private String address;

    @Column(name = "LATITUDE", nullable = false, precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(name = "LONGITUDE", nullable = false, precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(name = "CAPACITY", nullable = false)
    private Integer capacity;

    @Column(name = "OCCUPIED_SPOTS", nullable = false)
    private Integer occupiedSpots;

    /** Exigido pelo JPA. */
    protected Shelter() {
    }

    public Shelter(String name, String address, BigDecimal latitude, BigDecimal longitude,
                   Integer capacity, Integer occupiedSpots) {
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.capacity = capacity;
        this.occupiedSpots = occupiedSpots;
    }

    /** Vagas livres: o que a rota (RN06) usa para escolher um abrigo com vaga. */
    public int getAvailableSpots() {
        return capacity - occupiedSpots;
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
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

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Integer getOccupiedSpots() {
        return occupiedSpots;
    }

    public void setOccupiedSpots(Integer occupiedSpots) {
        this.occupiedSpots = occupiedSpots;
    }
}
