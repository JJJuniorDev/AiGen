// Model/Content.java (se non esiste)
package model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "content_history")
public class Content {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String content;
    
    @Column(name = "brand_id")
    private Long brandId;
    
    private String platform;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    // Constructors, getters, setters
    public Content() {}
    
    public Content(String content, Long brandId, String platform) {
        this.content = content;
        this.brandId = brandId;
        this.platform = platform;
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public Long getBrandId() { return brandId; }
    public void setBrandId(Long brandId) { this.brandId = brandId; }
    
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}