package model;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "testimonials")
public class Testimonial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Lob
    private String inputText;

    @ElementCollection
    @CollectionTable(name = "testimonial_linkedin_versions", joinColumns = @JoinColumn(name = "testimonial_id"))
    @Column(name = "version")
    private List<String> linkedinPostVersions;
    
    @ElementCollection
    @CollectionTable(name = "testimonial_headline_versions", joinColumns = @JoinColumn(name = "testimonial_id"))
    @Column(name = "version")
    private List<String> headlineVersions;
    
    @ElementCollection
    @CollectionTable(name = "testimonial_shortquote_versions", joinColumns = @JoinColumn(name = "testimonial_id"))
    @Column(name = "version")
    private List<String> shortQuoteVersions;

    private boolean exportedMd = false;
    private boolean exportedPng = false;

    private LocalDateTime createdAt = LocalDateTime.now();

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

	public String getInputText() {
		return inputText;
	}

	public void setInputText(String inputText) {
		this.inputText = inputText;
	}


	public List<String> getLinkedinPostVersions() {
		return linkedinPostVersions;
	}

	public void setLinkedinPostVersions(List<String> linkedinPostVersions) {
		this.linkedinPostVersions = linkedinPostVersions;
	}

	public List<String> getHeadlineVersions() {
		return headlineVersions;
	}

	public void setHeadlineVersions(List<String> headlineVersions) {
		this.headlineVersions = headlineVersions;
	}

	public List<String> getShortQuoteVersions() {
		return shortQuoteVersions;
	}

	public void setShortQuoteVersions(List<String> shortQuoteVersions) {
		this.shortQuoteVersions = shortQuoteVersions;
	}

	public boolean isExportedMd() {
		return exportedMd;
	}

	public void setExportedMd(boolean exportedMd) {
		this.exportedMd = exportedMd;
	}

	public boolean isExportedPng() {
		return exportedPng;
	}

	public void setExportedPng(boolean exportedPng) {
		this.exportedPng = exportedPng;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

   
}