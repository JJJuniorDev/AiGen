package DTO;

import java.util.List;

import Enums.ToneType;

public class AvatarDTO {
	 private Long id;
	    private String name;
	    private String description;
	    private String icon;
	    private int defaultEmotion;
	    private int defaultCreativity;
	    private int defaultFormality;
	    private int defaultUrgency;
	    private int defaultLength;
	    private ToneType defaultTone;
	    private String category;
	    private String biography;
	    private List<String> characteristicPhrases;
		public Long getId() {
			return id;
		}
		public void setId(Long id) {
			this.id = id;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getDescription() {
			return description;
		}
		public void setDescription(String description) {
			this.description = description;
		}
		public String getIcon() {
			return icon;
		}
		public void setIcon(String icon) {
			this.icon = icon;
		}
		public int getDefaultEmotion() {
			return defaultEmotion;
		}
		public void setDefaultEmotion(int defaultEmotion) {
			this.defaultEmotion = defaultEmotion;
		}
		public int getDefaultCreativity() {
			return defaultCreativity;
		}
		public void setDefaultCreativity(int defaultCreativity) {
			this.defaultCreativity = defaultCreativity;
		}
		public int getDefaultFormality() {
			return defaultFormality;
		}
		public void setDefaultFormality(int defaultFormality) {
			this.defaultFormality = defaultFormality;
		}
		public int getDefaultUrgency() {
			return defaultUrgency;
		}
		public void setDefaultUrgency(int defaultUrgency) {
			this.defaultUrgency = defaultUrgency;
		}
		public int getDefaultLength() {
			return defaultLength;
		}
		public void setDefaultLength(int defaultLength) {
			this.defaultLength = defaultLength;
		}
		public ToneType getDefaultTone() {
			return defaultTone;
		}
		public void setDefaultTone(ToneType defaultTone) {
			this.defaultTone = defaultTone;
		}
		public String getCategory() {
			return category;
		}
		public void setCategory(String category) {
			this.category = category;
		}
		public String getBiography() {
			return biography;
		}
		public void setBiography(String biography) {
			this.biography = biography;
		}
		public List<String> getCharacteristicPhrases() {
			return characteristicPhrases;
		}
		public void setCharacteristicPhrases(List<String> characteristicPhrases) {
			this.characteristicPhrases = characteristicPhrases;
		}
	
	    
}

