package DTO.Assistant;
public class ContentSuggestion {
 private String type;
 private String current;
 private String suggested;
 private String reason;
 
 // Constructors
 public ContentSuggestion() {}
 
 public ContentSuggestion(String type, String current, String suggested, String reason) {
     this.type = type;
     this.current = current;
     this.suggested = suggested;
     this.reason = reason;
 }
 
 // Getters & Setters
 public String getType() { return type; }
 public void setType(String type) { this.type = type; }
 
 public String getCurrent() { return current; }
 public void setCurrent(String current) { this.current = current; }
 
 public String getSuggested() { return suggested; }
 public void setSuggested(String suggested) { this.suggested = suggested; }
 
 public String getReason() { return reason; }
 public void setReason(String reason) { this.reason = reason; }
}