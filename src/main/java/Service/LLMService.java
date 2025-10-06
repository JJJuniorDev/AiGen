package Service;


import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import DTO.TestimonialDTO;

@Service
public class LLMService {
	
	@Value("${groq.api.key}")
    private String groqApiKey;
	
	private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

	private final ObjectMapper mapper = new ObjectMapper();
	
	public TestimonialDTO generate(String inputText, String platform, String postType, int tone, int style) {
	    RestTemplate restTemplate = new RestTemplate();

	    String styleHint = switch (platform.toLowerCase()) {
	        case "twitter" -> "Crea un tweet breve e d’impatto (massimo 280 caratteri)";
	        case "instagram" -> "Crea una caption emozionale e coinvolgente per Instagram, con emoji e hashtag rilevanti";
	        default -> "Crea un post professionale per LinkedIn con tono autorevole ma umano";
	    };

	    String prompt = """
	        Genera da questa testimonianza un contenuto ottimizzato per %s.
	        Tipo di post: "%s"
	        Tono (0-100): "%d"
	        Stile (0-100): "%d"
	        
	        Testimonianza: "%s"

	        ⚠️ Rispondi SOLO con JSON valido (senza testo o commenti esterni).
            ⚠️ Ogni campo deve contenere ESATTAMENTE 3 versioni, numerate da 1 a 3.
            Esempio di formato JSON atteso:
	        {
	          "linkedinPostVersions": [
	            "Versione 1",
	            "Versione 2",
	            "Versione 3"
	          ],
	          "headlineVersions": [
	            "Versione 1",
	            "Versione 2",
	            "Versione 3"
	          ],
	          "shortQuoteVersions": [
	            "Versione 1",
	            "Versione 2",
	            "Versione 3"
	          ]
	        }
	        """.formatted(platform, postType, tone, style, inputText);

	    HttpHeaders headers = new HttpHeaders();
	    headers.setContentType(MediaType.APPLICATION_JSON);
	    headers.setBearerAuth(groqApiKey);

	    Map<String, Object> body = Map.of(
	            "model", "llama-3.1-8b-instant",
	            "messages", new Object[]{
	                    Map.of("role", "system", "content", "Sei un copywriter esperto di social media marketing."),
	                    Map.of("role", "user", "content", prompt)
	            },
	            "temperature", 0.9,
	            "max_tokens", 800
	    );

	    HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
	    ResponseEntity<Map> response = restTemplate.exchange(GROQ_URL, HttpMethod.POST, request, Map.class);

	    String content = (String) ((Map) ((Map) ((List<?>) response.getBody().get("choices")).get(0)).get("message")).get("content");

	    TestimonialDTO dto = new TestimonialDTO();
	    dto.setInputText(inputText);
	    dto.setLinkedinPostVersions(extractList(content, "linkedinPostVersions"));
	    dto.setHeadlineVersions(extractList(content, "headlineVersions"));
	    dto.setShortQuoteVersions(extractList(content, "shortQuoteVersions"));
	    return dto;
	}

    private String extract(String text, String key) {
    	 try {
             int idx = text.indexOf("\"" + key + "\"");
             if (idx == -1) return "";
             int start = text.indexOf(":", idx) + 1;
             int end = text.indexOf(",", start);
             if (end == -1) end = text.length();
             String raw = text.substring(start, end).replaceAll("[{}\"\n]", "").trim();
             return raw;
         } catch (Exception e) {
             return "";
         }
     }
    /** ✅ Metodo robusto con ObjectMapper */
    private List<String> extractList(String text, String key) {
        try {
            JsonNode root = mapper.readTree(text);
            JsonNode arr = root.get(key);
            if (arr != null && arr.isArray()) {
                return mapper.convertValue(arr, List.class);
            }
            return Collections.emptyList();
        } catch (Exception e) {
            System.err.println("⚠️ Errore parsing JSON per chiave " + key + ": " + e.getMessage());
            return Collections.emptyList();
        }
    }

}