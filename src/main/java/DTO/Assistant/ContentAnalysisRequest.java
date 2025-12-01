package DTO.Assistant;

import Enums.AnalysisType;

public class ContentAnalysisRequest {
    private String content;
    private String brandId;
    private String platform;
    private AnalysisType analysisType;
    private boolean includeSuggestions = true;
    
    // Constructors
    public ContentAnalysisRequest() {}
    
    public ContentAnalysisRequest(String content, String brandId, String platform, 
                                AnalysisType analysisType, boolean includeSuggestions) {
        this.content = content;
        this.brandId = brandId;
        this.platform = platform;
        this.analysisType = analysisType;
        this.includeSuggestions = includeSuggestions;
    }
    
    // Getters & Setters
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public String getBrandId() { return brandId; }
    public void setBrandId(String brandId) { this.brandId = brandId; }
    
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    
    public AnalysisType getAnalysisType() { return analysisType; }
    public void setAnalysisType(AnalysisType analysisType) { this.analysisType = analysisType; }
    
    public boolean isIncludeSuggestions() { return includeSuggestions; }
    public void setIncludeSuggestions(boolean includeSuggestions) { this.includeSuggestions = includeSuggestions; }
}