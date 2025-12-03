// SocialImageRequest.java - VERSIONE ULTRA-SEMPLICE
package DTO;

public class SocialImageRequest {
    private String content;
    private String platform;
    private String brandName;
    private String primaryColor;
    private boolean includeText;
    private String style; 
    
    // Costruttori
    public SocialImageRequest() {}
    
    // Getters e Setters
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    
    public String getBrandName() { return brandName; }
    public void setBrandName(String brandName) { this.brandName = brandName; }
    
    public String getPrimaryColor() { return primaryColor; }
    public void setPrimaryColor(String primaryColor) { this.primaryColor = primaryColor; }
    
    public boolean isIncludeText() { return includeText; }
    public void setIncludeText(boolean includeText) { this.includeText = includeText; }

	public String getStyle() {
		return style;
	}

	public void setStyle(String style) {
		this.style = style;
	}
    
    
}