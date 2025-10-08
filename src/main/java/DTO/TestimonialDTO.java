package DTO;

import java.time.LocalDateTime;
import java.util.List;

public class TestimonialDTO {
    private String id;
    private String userId;
    private String inputText;
    private List<String> linkedinPostVersions;
    private List<String> headlineVersions;
    private List<String> shortQuoteVersions;
    private boolean exportedMd;
    private boolean exportedPng;
    private LocalDateTime createdAt;
    private String platform;
    private String selectedPostType;
    private int emotion;
    private int creativity;
    private int formality;
    private int urgency;
    private int length;
    private String brandProfileId;
    
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
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
	public String getPlatform() {
		return platform;
	}
	public void setPlatform(String platform) {
		this.platform = platform;
	}
	public String getSelectedPostType() {
		return selectedPostType;
	}
	public void setSelectedPostType(String selectedPostType) {
		this.selectedPostType = selectedPostType;
	}
	public int getEmotion() {
		return emotion;
	}
	public void setEmotion(int emotion) {
		this.emotion = emotion;
	}
	public int getCreativity() {
		return creativity;
	}
	public void setCreativity(int creativity) {
		this.creativity = creativity;
	}
	public int getFormality() {
		return formality;
	}
	public void setFormality(int formality) {
		this.formality = formality;
	}
	public int getUrgency() {
		return urgency;
	}
	public void setUrgency(int urgency) {
		this.urgency = urgency;
	}
	public int getLength() {
		return length;
	}
	public void setLength(int length) {
		this.length = length;
	}
	public String getBrandProfileId() {
		return brandProfileId;
	}
	public void setBrandProfileId(String brandProfileId) {
		this.brandProfileId = brandProfileId;
	}
	

   
}
