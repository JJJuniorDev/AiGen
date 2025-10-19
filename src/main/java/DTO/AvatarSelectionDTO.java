package DTO;

public class AvatarSelectionDTO {
    private Long avatarId;
    private Long brandProfileId; // Opzionale, per associare avatar a brand
    
    // Parametri override (se l'utente vuole modificare i default)
    private Integer customEmotion;
    private Integer customCreativity;
    private Integer customFormality;
    private Integer customUrgency;
    private Integer customLength;
	public Long getAvatarId() {
		return avatarId;
	}
	public void setAvatarId(Long avatarId) {
		this.avatarId = avatarId;
	}
	public Long getBrandProfileId() {
		return brandProfileId;
	}
	public void setBrandProfileId(Long brandProfileId) {
		this.brandProfileId = brandProfileId;
	}
	public Integer getCustomEmotion() {
		return customEmotion;
	}
	public void setCustomEmotion(Integer customEmotion) {
		this.customEmotion = customEmotion;
	}
	public Integer getCustomCreativity() {
		return customCreativity;
	}
	public void setCustomCreativity(Integer customCreativity) {
		this.customCreativity = customCreativity;
	}
	public Integer getCustomFormality() {
		return customFormality;
	}
	public void setCustomFormality(Integer customFormality) {
		this.customFormality = customFormality;
	}
	public Integer getCustomUrgency() {
		return customUrgency;
	}
	public void setCustomUrgency(Integer customUrgency) {
		this.customUrgency = customUrgency;
	}
	public Integer getCustomLength() {
		return customLength;
	}
	public void setCustomLength(Integer customLength) {
		this.customLength = customLength;
	}
    
    // Costruttori, getter e setter
    
    
}
