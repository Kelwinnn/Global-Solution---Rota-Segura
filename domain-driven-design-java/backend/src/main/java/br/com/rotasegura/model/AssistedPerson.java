package br.com.rotasegura.model;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Pessoa assistida (RF08): quem não consegue sair sozinha numa emergência. Tem área e responsáveis (RN03). */
@Entity
@Table(name = "T_RS_ASSISTED_PERSON")
public class AssistedPerson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_ASSISTED_PERSON")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ID_AREA", nullable = false, foreignKey = @ForeignKey(name = "FK_RS_ASSISTED_AREA"))
    private RiskArea area;

    @Column(name = "NAME", nullable = false, length = 150)
    private String name;

    @Column(name = "PHONE", nullable = false, length = 20)
    private String phone;

    /** Onde a pessoa mora: é a origem da rota até o abrigo (RF07). */
    @Column(name = "LATITUDE", nullable = false, precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(name = "LONGITUDE", nullable = false, precision = 9, scale = 6)
    private BigDecimal longitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "SPECIAL_NEED", nullable = false, length = 20)
    private SpecialNeed specialNeed;

    @Column(name = "NOTES", length = 500)
    private String notes;

    /**
     * Os responsáveis "moram" dentro do assistido:
     * cascade ALL grava e exclui junto; orphanRemoval apaga o responsável tirado da lista.
     */
    @OneToMany(mappedBy = "assistedPerson", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<Guardian> guardians = new ArrayList<>();

    /** Exigido pelo JPA. */
    protected AssistedPerson() {
    }

    public AssistedPerson(RiskArea area, String name, String phone, BigDecimal latitude, BigDecimal longitude,
                          SpecialNeed specialNeed, String notes) {
        this.area = area;
        this.name = name;
        this.phone = phone;
        this.latitude = latitude;
        this.longitude = longitude;
        this.specialNeed = specialNeed;
        this.notes = notes;
    }

    /** Troca a lista inteira de responsáveis, mantendo os dois lados da ligação certos. */
    public void replaceGuardians(List<Guardian> newGuardians) {
        guardians.forEach(guardian -> guardian.setAssistedPerson(null));
        guardians.clear();
        newGuardians.forEach(guardian -> {
            guardian.setAssistedPerson(this);
            guardians.add(guardian);
        });
    }

    /** Só leitura: para mudar, use replaceGuardians. */
    public List<Guardian> getGuardians() {
        return Collections.unmodifiableList(guardians);
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
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

    public SpecialNeed getSpecialNeed() {
        return specialNeed;
    }

    public void setSpecialNeed(SpecialNeed specialNeed) {
        this.specialNeed = specialNeed;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
