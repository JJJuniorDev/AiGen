package DTO;

public class GenerationResponse {
    private String linkedinPost;
    private String headline;
    private String shortQuote;

    public GenerationResponse() {}

    public GenerationResponse(String linkedinPost, String headline, String shortQuote) {
        this.linkedinPost = linkedinPost;
        this.headline = headline;
        this.shortQuote = shortQuote;
    }

    public String getLinkedinPost() { return linkedinPost; }
    public void setLinkedinPost(String linkedinPost) { this.linkedinPost = linkedinPost; }

    public String getHeadline() { return headline; }
    public void setHeadline(String headline) { this.headline = headline; }

    public String getShortQuote() { return shortQuote; }
    public void setShortQuote(String shortQuote) { this.shortQuote = shortQuote; }
}