package Enums;

public enum AnalysisType {
	 TONE_CONSISTENCY("Coerenza Tono"),
	    ENGAGEMENT_OPTIMIZATION("Ottimizzazione Engagement"),
	    BRAND_ALIGNMENT("Allineamento Brand"),
	    PLATFORM_OPTIMIZATION("Ottimizzazione Piattaforma");
	    
	    private final String displayName;
	    
	    AnalysisType(String displayName) {
	        this.displayName = displayName;
	    }
	    
	    public String getDisplayName() {
	        return displayName;
	    }
}
