// SocialImageResponse.java - VERSIONE ULTRA-SEMPLICE
package DTO;

import java.util.Date;
import java.util.Map;

public class SocialImageResponse {
    private String imageUrl;
    private String imageBase64;
    private String promptUsed;
    private String platform;
    private Map<String, Integer> dimensions;
    private Date generatedAt;
    private boolean savedToCloudinary = false; // Flag per indicare se è stato fatto upload
    private String temporaryId;
    // Costruttori
    public SocialImageResponse() {}
    
    
	public SocialImageResponse(String imageUrl, String imageBase64, String promptUsed, String platform,
			Map<String, Integer> dimensions, Date generatedAt, boolean savedToCloudinary, String temporaryId) {
		super();
		this.imageUrl = imageUrl;
		this.imageBase64 = imageBase64;
		this.promptUsed = promptUsed;
		this.platform = platform;
		this.dimensions = dimensions;
		this.generatedAt = generatedAt;
		this.savedToCloudinary = savedToCloudinary;
		this.temporaryId = temporaryId;
	}


	public String getImageUrl() {
		return imageUrl;
	}
	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}
	public String getImageBase64() {
		return imageBase64;
	}
	public void setImageBase64(String imageBase64) {
		this.imageBase64 = imageBase64;
	}
	public String getPromptUsed() {
		return promptUsed;
	}
	public void setPromptUsed(String promptUsed) {
		this.promptUsed = promptUsed;
	}
	public String getPlatform() {
		return platform;
	}
	public void setPlatform(String platform) {
		this.platform = platform;
	}
	public Map<String, Integer> getDimensions() {
		return dimensions;
	}
	public void setDimensions(Map<String, Integer> dimensions) {
		this.dimensions = dimensions;
	}
	public Date getGeneratedAt() {
		return generatedAt;
	}
	public void setGeneratedAt(Date generatedAt) {
		this.generatedAt = generatedAt;
	}
	public boolean isSavedToCloudinary() {
		return savedToCloudinary;
	}
	public void setSavedToCloudinary(boolean savedToCloudinary) {
		this.savedToCloudinary = savedToCloudinary;
	}
	public String getTemporaryId() {
		return temporaryId;
	}
	public void setTemporaryId(String temporaryId) {
		this.temporaryId = temporaryId;
	}
    
   
}