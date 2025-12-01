package DTO.Assistant;

import java.util.List;

public class AssistantResponse {
 private List<String> strengths;
 private List<String> improvements;
 private List<ContentSuggestion> suggestions;
 private List<String> platformTips;
 private int qualityScore;
 private double confidence;
 
 // Constructors
 public AssistantResponse() {}
 
 public AssistantResponse(List<String> strengths, List<String> improvements, 
                        List<ContentSuggestion> suggestions, List<String> platformTips,
                        int qualityScore, double confidence) {
     this.strengths = strengths;
     this.improvements = improvements;
     this.suggestions = suggestions;
     this.platformTips = platformTips;
     this.qualityScore = qualityScore;
     this.confidence = confidence;
 }
 
 // Getters & Setters
 public List<String> getStrengths() { return strengths; }
 public void setStrengths(List<String> strengths) { this.strengths = strengths; }
 
 public List<String> getImprovements() { return improvements; }
 public void setImprovements(List<String> improvements) { this.improvements = improvements; }
 
 public List<ContentSuggestion> getSuggestions() { return suggestions; }
 public void setSuggestions(List<ContentSuggestion> suggestions) { this.suggestions = suggestions; }
 
 public List<String> getPlatformTips() { return platformTips; }
 public void setPlatformTips(List<String> platformTips) { this.platformTips = platformTips; }
 
 public int getQualityScore() { return qualityScore; }
 public void setQualityScore(int qualityScore) { this.qualityScore = qualityScore; }
 
 public double getConfidence() { return confidence; }
 public void setConfidence(double confidence) { this.confidence = confidence; }
}