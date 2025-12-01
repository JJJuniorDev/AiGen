// WebsiteAnalysisService.java
package Service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import Enums.ToneType;
import model.BrandProfile;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class WebsiteAnalysisService {

    @Autowired
    private LLMService llmService;

    private static final Pattern URL_PATTERN = Pattern.compile(
        "^(https?://)?([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}(/.*)?$"
    );

    public BrandProfile analyzeWebsiteAndCreateBrand(String websiteUrl, String language) {
        try {
            // 1. Estrazione contenuto del sito
            String websiteContent = extractWebsiteContent(websiteUrl);
            
            // 2. Analisi con AI per creare il profilo brand
            return createBrandProfileFromAnalysis(websiteContent, websiteUrl, language);
            
        } catch (Exception e) {
            throw new RuntimeException("Errore nell'analisi del sito: " + e.getMessage());
        }
    }

    private String extractWebsiteContent(String url) {
        try {
            // Normalizza URL
            if (!url.startsWith("http")) {
                url = "https://" + url;
            }

            System.out.println("🌐 Scraping sito: " + url);
            
            Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (compatible; SocialCraft-Bot/1.0; +https://socialcraft.ai)")
                .timeout(10000)
                .get();

            // Estrai contenuti rilevanti in ordine di importanza
            StringBuilder content = new StringBuilder();

            // 1. Meta tag più importanti
            content.append("=== META INFORMATION ===\n");
            content.append("Title: ").append(doc.title()).append("\n");
            content.append("Description: ").append(doc.select("meta[name=description]").attr("content")).append("\n");
            content.append("Keywords: ").append(doc.select("meta[name=keywords]").attr("content")).append("\n");

            // 2. Contenuto principale (h1, h2, h3, paragrafi)
            content.append("\n=== MAIN CONTENT ===\n");
            doc.select("h1, h2, h3, p, li").forEach(element -> {
                String text = element.text().trim();
                if (text.length() > 10 && text.length() < 500) {
                    content.append(element.tagName()).append(": ").append(text).append("\n");
                }
            });

            // 3. Testo di header/footer per valori del brand
            content.append("\n=== BRAND ELEMENTS ===\n");
            doc.select("header, footer, nav, .brand, .logo, .slogan").forEach(element -> {
                String text = element.text().trim();
                if (!text.isEmpty()) {
                    content.append("Brand Element: ").append(text).append("\n");
                }
            });

            String fullContent = content.toString();
            
            // Limita a 4000 caratteri per costi API
            if (fullContent.length() > 4000) {
                fullContent = fullContent.substring(0, 4000) + "... [truncated]";
            }

            System.out.println("✅ Contenuto estratto: " + fullContent.length() + " caratteri");
            return fullContent;

        } catch (Exception e) {
            System.err.println("❌ Errore scraping: " + e.getMessage());
            // Fallback: ritorna almeno l'URL per analisi di base
            return "Sito web: " + url + "\nImpossibile estrarre contenuto dettagliato.";
        }
    }

    private BrandProfile createBrandProfileFromAnalysis(String websiteContent, String websiteUrl, String language) {
        String prompt = buildAnalysisPrompt(websiteContent, websiteUrl, language);
        
        System.out.println("🤖 Analisi AI del sito...");
        String analysisResult = llmService.callGroqAPIForAnalysis(prompt, "Website Analysis", 3000);
        return parseBrandProfileFromAnalysis(analysisResult, websiteUrl);
    }

    private String buildAnalysisPrompt(String websiteContent, String websiteUrl, String language) {
        if ("it".equals(language)) {
            return """
                Analizza questo contenuto estratto da un sito web e crea un profilo brand COMPLETO e PRONTO ALL'USO.
                
                CONTENUTO SITO WEB:
                %s
                
                URL: %s
                
                ISTRUZIONI DETTAGLIATE:
                
                1. **NOME BRAND**: Estrai il nome principale del brand/azienda
                2  **POSIZIONAMENTO DI MERCATO** Estrapola come il brand/azienda si identifica nel mercato
                3. **DESCRIZIONE**: Crea una descrizione precisa e bella che che spiega cosa fa il brand
                4. **TONO DI VOCE**: Identifica tra questi:
                   - FORMALE_PROFESSIONALE (siti corporate, B2B, servizi professionali)
                   - CASUALE_FRIENDLY (brand giovani, lifestyle, B2C)
                   - ENTUSIASTA_ENERGETICO (sport, fitness, motivazione)
                   - TECNICO_DETTAGLIATO (tech, software, ingegneria)
                   - MOTIVAZIONALE_ISPIRAZIONE (coaching, sviluppo personale)
                   - EDUCATIVO_INFORMATIVO (scuole, formazione, contenuti educativi)
                5. **TARGET AUDIENCE**: Definisci il pubblico principale basato sul contenuto
                6. **VALORI BRAND**: Estrai 3-5 valori chiave (es: Innovazione, Qualità, Sostenibilità)
                7. **TAGLINE**: Crea uno slogan breve e memorabile se possibile
                8. **KEYWORDS**: Estrai 5-8 parole chiave ricorrenti o rilevanti
                9. **HASHTAG**: Suggerisci 3-5 hashtag pertinenti
                
                OUTPUT FORMATO JSON:
                {
                  "brandName": "Nome del Brand",
                  "positioning": "Posizionamento di mercato",
                  "brandDescription": "Descrizione concisa...",
                  "tone": "FORMALE_PROFESSIONALE",
                  "targetAudience": "Professionisti 30-50 anni...",
                  "brandValues": "Innovazione, Qualità, Affidabilità",
                  "tagline": "Slogan breve e memorabile",
                  "preferredKeywords": ["keyword1", "keyword2", "keyword3"],
                  "defaultHashtags": ["#hashtag1", "#hashtag2"]
                }
                
                Sii preciso e basati SOLO sul contenuto fornito.
                """.formatted(websiteContent, websiteUrl);
        } else {
            return """
                Analyze this content extracted from a website and create a COMPLETE, READY-TO-USE brand profile.
                
                WEBSITE CONTENT:
                %s
                
                URL: %s
                
                DETAILED INSTRUCTIONS:
                
                1. **BRAND NAME**: Extract the main brand/company name
                2. **DESCRIPTION**: Create a concise description (max 200 chars) explaining what the brand does
                3. **VOICE TONE**: Identify among these:
                   - FORMALE_PROFESSIONALE (corporate sites, B2B, professional services)
                   - CASUALE_FRIENDLY (young brands, lifestyle, B2C)
                   - ENTUSIASTA_ENERGETICO (sports, fitness, motivation)
                   - TECNICO_DETTAGLIATO (tech, software, engineering)
                   - MOTIVAZIONALE_ISPIRAZIONE (coaching, personal development)
                   - EDUCATIVO_INFORMATIVO (schools, education, educational content)
                4. **TARGET AUDIENCE**: Define the primary audience based on content
                5. **BRAND VALUES**: Extract 3-5 key values (ex: Innovation, Quality, Sustainability)
                6. **TAGLINE**: Create a short memorable slogan if possible
                7. **KEYWORDS**: Extract 5-8 recurring or relevant keywords
                8. **HASHTAGS**: Suggest 3-5 relevant hashtags
                
                JSON OUTPUT FORMAT:
                {
                  "brandName": "Brand Name",
                  "brandDescription": "Concise description...",
                  "tone": "FORMALE_PROFESSIONALE",
                  "targetAudience": "Professionals 30-50 years...",
                  "brandValues": "Innovation, Quality, Reliability",
                  "tagline": "Short memorable slogan",
                  "preferredKeywords": ["keyword1", "keyword2", "keyword3"],
                  "defaultHashtags": ["#hashtag1", "#hashtag2"]
                }
                
                Be precise and base ONLY on the provided content.
                """.formatted(websiteContent, websiteUrl);
        }
    }

    private BrandProfile parseBrandProfileFromAnalysis(String analysisResult, String websiteUrl) {
        try {
            // Pulisci la risposta JSON
            String cleanJson = analysisResult
                .replaceAll("(?i)```json", "")
                .replaceAll("```", "")
                .trim();

            // Usa Jackson per parsare il JSON
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(cleanJson);

            BrandProfile profile = new BrandProfile();
            profile.setBrandName(root.has("brandName") ? root.get("brandName").asText() : "Brand da Sito Web");
            profile.setBrandDescription(root.has("brandDescription") ? root.get("brandDescription").asText() : "");
            profile.setTone(root.has("tone") ? ToneType.valueOf(root.get("tone").asText()) : ToneType.FORMALE_PROFESSIONALE);
            profile.setTargetAudience(root.has("targetAudience") ? root.get("targetAudience").asText() : "");
            profile.setBrandValues(root.has("brandValues") ? root.get("brandValues").asText() : "");
            profile.setTagline(root.has("tagline") ? root.get("tagline").asText() : "");
            profile.setPositioning(root.has("positioning") ? root.get("positioning").asText(): "");            // Keywords
            if (root.has("preferredKeywords")) {
                List<String> keywords = new ArrayList<>();
                root.get("preferredKeywords").forEach(node -> keywords.add(node.asText()));
                profile.setPreferredKeywords(keywords);
            } else {
                profile.setPreferredKeywords(List.of("qualità", "innovazione", "professionale"));
            }
            
            // Hashtags
            if (root.has("defaultHashtags")) {
                List<String> hashtags = new ArrayList<>();
                root.get("defaultHashtags").forEach(node -> hashtags.add(node.asText()));
                profile.setDefaultHashtags(hashtags);
            }
            
            // Campi aggiuntivi
            profile.setVisualStyle("Moderno e Professionale");
            profile.setColorPalette("#000000, #FFFFFF");
            profile.setAvoidedWords(List.of());
            profile.setPreferredCTAs(List.of("Scopri di più", "Contattaci", "Visita il sito"));

            System.out.println("✅ Profilo brand creato: " + profile.getBrandName());
            return profile;

        } catch (Exception e) {
            System.err.println("❌ Errore parsing analisi: " + e.getMessage());
            // Fallback: profilo generico
            return null;
          //  return createFallbackBrandProfile(websiteUrl);
        }
    }

    private BrandProfile createFallbackBrandProfile(String websiteUrl) {
        BrandProfile profile = new BrandProfile();
        profile.setBrandName("Brand da " + websiteUrl);
        profile.setBrandDescription("Brand creato automaticamente dall'analisi del sito web");
        profile.setTone(ToneType.FORMALE_PROFESSIONALE);
        profile.setTargetAudience("Clienti del sito web");
        profile.setBrandValues("Professionalità, Qualità, Innovazione");
        profile.setPreferredKeywords(List.of("qualità", "servizio", "professionale"));
        profile.setDefaultHashtags(List.of("#business", "#professional"));
        return profile;
    }
}