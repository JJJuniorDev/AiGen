// SocialImageBatchRequest.java
package DTO;

import java.util.List;

public class SocialImageBatchRequest {
    private List<String> posts;
    private String platform;
    private String brandName;
    private String style;
    private boolean includeText;
    
    // Costruttori
    public SocialImageBatchRequest() {}
    
    // Getters e Setters
    public List<String> getPosts() { return posts; }
    public void setPosts(List<String> posts) { this.posts = posts; }
    
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    
    public String getBrandName() { return brandName; }
    public void setBrandName(String brandName) { this.brandName = brandName; }
    
    public String getStyle() { return style; }
    public void setStyle(String style) { this.style = style; }
    
    public boolean isIncludeText() { return includeText; }
    public void setIncludeText(boolean includeText) { this.includeText = includeText; }
}