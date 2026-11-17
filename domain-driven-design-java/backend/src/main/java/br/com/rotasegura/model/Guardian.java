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

/** Responsável por uma pessoa assistida (RF08, RN03). Só existe junto do assistido dele. */
@Entity
@Table(name = "T_RS_GUARDIAN")
public class Guardian {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_GUARDIAN")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ID_ASSISTED_PERSON", nullable = false,
            foreignKey = @ForeignKey(name = "FK_RS_GUARDIAN_ASSISTED"))
    private AssistedPerson assistedPerson;

    @Column(name = "NAME", nullable = false, length = 150)
    private String name;

    @Column(name = "PHONE", nullable = false, length = 20)
    private String phone;

    @Column(name = "RELATIONSHIP", nullable = false, length = 50)
    private String relationship;

    /** Exigido pelo JPA. */
    protected Guardian() {
    }

    public Guardian(String name, String phone, String relationship) {
        this.name = name;
        this.phone = phone;
        this.relationship = relationship;
    }

    public Long getId() {
        return id;
    }

    public AssistedPerson getAssistedPerson() {
        return assistedPerson;
    }

    /** Usado só por AssistedPerson, que mantém os dois lados da ligação iguais. */
    void setAssistedPerson(AssistedPerson assistedPerson) {
        this.assistedPerson = assistedPerson;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public String getRelationship() {
        return relationship;
    }
}
