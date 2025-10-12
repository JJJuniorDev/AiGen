package model;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import Enums.ToneType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "brand_profiles")
public class BrandProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String brandName;

    // === TONO E STILE ===
    @Enumerated(EnumType.STRING)
    private ToneType tone; // FORMAL, CASUAL, ENTHUSIASTIC, TECHNICAL, etc.

    @ElementCollection
    @CollectionTable(name = "brand_keywords", joinColumns = @JoinColumn(name = "brand_profile_id"))
    private List<String> preferredKeywords;

    @ElementCollection
    @CollectionTable(name = "brand_avoided_words", joinColumns = @JoinColumn(name = "brand_profile_id"))
    private List<String> avoidedWords;

    // === CONTESTO BRAND ===
    @Column(columnDefinition = "TEXT")
    private String brandDescription;

    @Column(columnDefinition = "TEXT")
    private String targetAudience;

    @Column(columnDefinition = "TEXT")
    private String brandValues; // "Innovation, Trust, Sustainability"

    // === ELEMENTI IDENTITARI ===
    private String tagline;
    
    @ElementCollection
    @CollectionTable(name = "brand_hashtags", joinColumns = @JoinColumn(name = "brand_profile_id"))
    private List<String> defaultHashtags;

    // === PREFERENZE VISIVE (per futuro) ===
    private String visualStyle; // "Minimal, Bold, Playful"
    private String colorPalette; // JSON: {"primary": "#FF0000", ...}

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @ElementCollection
    private List<String> preferredCTAs = Arrays.asList("Scopri di più", "Contattaci", "Inizia ora");
    
    private String positioning;
    
    
    
    
    
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public User getUser() {
		return user;
	}
	public void setUser(User user) {
		this.user = user;
	}
	public String getBrandName() {
		return brandName;
	}
	public void setBrandName(String brandName) {
		this.brandName = brandName;
	}
	public ToneType getTone() {
		return tone;
	}
	public void setTone(ToneType tone) {
		this.tone = tone;
	}
	public List<String> getPreferredKeywords() {
		return preferredKeywords;
	}
	public void setPreferredKeywords(List<String> preferredKeywords) {
		this.preferredKeywords = preferredKeywords;
	}
	public List<String> getAvoidedWords() {
		return avoidedWords;
	}
	public void setAvoidedWords(List<String> avoidedWords) {
		this.avoidedWords = avoidedWords;
	}
	public String getBrandDescription() {
		return brandDescription;
	}
	public void setBrandDescription(String brandDescription) {
		this.brandDescription = brandDescription;
	}
	public String getTargetAudience() {
		return targetAudience;
	}
	public void setTargetAudience(String targetAudience) {
		this.targetAudience = targetAudience;
	}
	public String getBrandValues() {
		return brandValues;
	}
	public void setBrandValues(String brandValues) {
		this.brandValues = brandValues;
	}
	public String getTagline() {
		return tagline;
	}
	public void setTagline(String tagline) {
		this.tagline = tagline;
	}
	public List<String> getDefaultHashtags() {
		return defaultHashtags;
	}
	public void setDefaultHashtags(List<String> defaultHashtags) {
		this.defaultHashtags = defaultHashtags;
	}
	public String getVisualStyle() {
		return visualStyle;
	}
	public void setVisualStyle(String visualStyle) {
		this.visualStyle = visualStyle;
	}
	public String getColorPalette() {
		return colorPalette;
	}
	public void setColorPalette(String colorPalette) {
		this.colorPalette = colorPalette;
	}
	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}
	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}
	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}
	public List<String> getPreferredCTAs() {
		return preferredCTAs;
	}
	public void setPreferredCTAs(List<String> preferredCTAs) {
		this.preferredCTAs = preferredCTAs;
	}
	public String getPositioning() {
		return positioning;
	}
	public void setPositioning(String positioning) {
		this.positioning = positioning;
	}
    
    
}