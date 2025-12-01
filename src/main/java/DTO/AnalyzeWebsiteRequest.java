package DTO;

public class AnalyzeWebsiteRequest {
    private String websiteUrl;
    private String language = "it";
    
    // getters & setters
    public String getWebsiteUrl() { return websiteUrl; }
    public void setWebsiteUrl(String websiteUrl) { this.websiteUrl = websiteUrl; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
}