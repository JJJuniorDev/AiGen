package DTO;

public class GenerationResponse {
    private String socialPost;
    private String headline;
    private String shortQuote;

    public GenerationResponse() {}

    public GenerationResponse(String socialPost, String headline, String shortQuote) {
        this.socialPost = socialPost;
        this.headline = headline;
        this.shortQuote = shortQuote;
    }

    public String getSocialPost() { return socialPost; }
    public void setSocialPost(String socialPost) { this.socialPost = socialPost; }

    public String getHeadline() { return headline; }
    public void setHeadline(String headline) { this.headline = headline; }

    public String getShortQuote() { return shortQuote; }
    public void setShortQuote(String shortQuote) { this.shortQuote = shortQuote; }
}