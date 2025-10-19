package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "credit_packages")
public class CreditPackage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // "Pacchetto Base", "Pacchetto Pro", etc.

    @Column(nullable = false)
    private String code; // "BASIC_50", "PRO_200", "AGENCY_1000"

    @Column(nullable = false)
    private Integer credits;

    @Column(nullable = false)
    private BigDecimal price; // Prezzo in EUR

    @Column(nullable = false)
    private Boolean active = true;

    private String description;
    
    @Column(nullable = false)
    private Integer validityDays = 365; // Validità crediti

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Costruttori
    public CreditPackage() {}

    public CreditPackage(String name, String code, Integer credits, BigDecimal price, String description) {
        this.name = name;
        this.code = code;
        this.credits = credits;
        this.price = price;
        this.description = description;
    }

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public Integer getCredits() {
		return credits;
	}

	public void setCredits(Integer credits) {
		this.credits = credits;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	public Boolean getActive() {
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Integer getValidityDays() {
		return validityDays;
	}

	public void setValidityDays(Integer validityDays) {
		this.validityDays = validityDays;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

    // Getters e Setters...
    
}