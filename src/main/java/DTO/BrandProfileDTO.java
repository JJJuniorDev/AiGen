package DTO;

import java.util.List;

import Enums.ToneType;

public class BrandProfileDTO {
    private String brandName;
    private ToneType tone;
    private List<String> preferredKeywords;
    private List<String> avoidedWords;
    private String brandDescription;
    private String targetAudience;
    private String brandValues;
    private String tagline;
    private List<String> defaultHashtags;
    private String visualStyle;
    private String colorPalette;
	public String getBrandName() {
		return brandName;
	}
	public void setBrandName(String brandName) {
		this.brandName = brandName;
	}
	public ToneType getTone() {
		return tone;
	}
	public void setTone(ToneType tone) {
		this.tone = tone;
	}
	public List<String> getPreferredKeywords() {
		return preferredKeywords;
	}
	public void setPreferredKeywords(List<String> preferredKeywords) {
		this.preferredKeywords = preferredKeywords;
	}
	public List<String> getAvoidedWords() {
		return avoidedWords;
	}
	public void setAvoidedWords(List<String> avoidedWords) {
		this.avoidedWords = avoidedWords;
	}
	public String getBrandDescription() {
		return brandDescription;
	}
	public void setBrandDescription(String brandDescription) {
		this.brandDescription = brandDescription;
	}
	public String getTargetAudience() {
		return targetAudience;
	}
	public void setTargetAudience(String targetAudience) {
		this.targetAudience = targetAudience;
	}
	public String getBrandValues() {
		return brandValues;
	}
	public void setBrandValues(String brandValues) {
		this.brandValues = brandValues;
	}
	public String getTagline() {
		return tagline;
	}
	public void setTagline(String tagline) {
		this.tagline = tagline;
	}
	public List<String> getDefaultHashtags() {
		return defaultHashtags;
	}
	public void setDefaultHashtags(List<String> defaultHashtags) {
		this.defaultHashtags = defaultHashtags;
	}
	public String getVisualStyle() {
		return visualStyle;
	}
	public void setVisualStyle(String visualStyle) {
		this.visualStyle = visualStyle;
	}
	public String getColorPalette() {
		return colorPalette;
	}
	public void setColorPalette(String colorPalette) {
		this.colorPalette = colorPalette;
	}

    
}