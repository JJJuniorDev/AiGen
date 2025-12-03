package DTO;

public class SaveImageRequest {
    private String imageBase64;
    private String platform;
    private String brandName;
    
    // Costruttori
    public SaveImageRequest() {}
    
    public SaveImageRequest(String imageBase64, String platform, String brandName) {
        this.imageBase64 = imageBase64;
        this.platform = platform;
        this.brandName = brandName;
    }
    
    // Getters e Setters
    public String getImageBase64() { return imageBase64; }
    public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }
    
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    
    public String getBrandName() { return brandName; }
    public void setBrandName(String brandName) { this.brandName = brandName; }
}