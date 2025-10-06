package DTO;

public class UserDTO {
    private String id;
    private String email;
    private String plan;
    private Integer credits;
    private String brandLogoUrl;
    private String primaryColor;
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getPlan() {
		return plan;
	}
	public void setPlan(String plan) {
		this.plan = plan;
	}
	public Integer getCredits() {
		return credits;
	}
	public void setCredits(Integer credits) {
		this.credits = credits;
	}
	public String getBrandLogoUrl() {
		return brandLogoUrl;
	}
	public void setBrandLogoUrl(String brandLogoUrl) {
		this.brandLogoUrl = brandLogoUrl;
	}
	public String getPrimaryColor() {
		return primaryColor;
	}
	public void setPrimaryColor(String primaryColor) {
		this.primaryColor = primaryColor;
	}


}