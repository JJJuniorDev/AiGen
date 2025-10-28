package model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String plan = "FREE";

    @Column( nullable = false)
    private Integer credits;

    private String brandLogoUrl;
    private String primaryColor;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    // ✅ NUOVI CAMPI
    @Column(nullable = false)
    private Integer maxBrands = 3; // Limite brand per piano FREE

    @Column(nullable = false)
    private Boolean active = true;

    private String stripeCustomerId; // Per i pagamenti
    
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public String getPlan() {
		return plan;
	}

	public void setPlan(String plan) {
		this.plan = plan;
	}

	public Integer getCredits() {
		return credits;
	}

	public void setCredits(Integer credits) {
		this.credits = credits;
	}

	public String getBrandLogoUrl() {
		return brandLogoUrl;
	}

	public void setBrandLogoUrl(String brandLogoUrl) {
		this.brandLogoUrl = brandLogoUrl;
	}

	public String getPrimaryColor() {
		return primaryColor;
	}

	public void setPrimaryColor(String primaryColor) {
		this.primaryColor = primaryColor;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public Integer getMaxBrands() {
		return maxBrands;
	}

	public void setMaxBrands(Integer maxBrands) {
		this.maxBrands = maxBrands;
	}

	public Boolean getActive() {
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}

	public String getStripeCustomerId() {
		return stripeCustomerId;
	}

	
	
	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}

	public void setStripeCustomerId(String stripeCustomerId) {
		this.stripeCustomerId = stripeCustomerId;
	}

	 public void updateMaxBrandsBasedOnPlan() {
	        switch (this.plan) {
	            case "FREE":
	                this.maxBrands = 3;
	                break;
	            case "STARTER":
	                this.maxBrands = 10;
	                break;
	            case "PRO":
	                this.maxBrands = 50;
	                break;
	            case "ENTERPRISE":
	                this.maxBrands = 1000; // Quasi illimitato
	                break;
	            default:
	                this.maxBrands = 3;
	        }
	    }
}
