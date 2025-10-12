package Service;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import model.BrandProfile;

@Service
public class LLMService {
    
    @Value("${groq.api.key}")
    private String groqApiKey;
    
    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

    private final ObjectMapper mapper = new ObjectMapper();
    
    public TestimonialDTO generate(String inputText, String platform, String postType,
            int emotion, int creativity, int formality, int urgency, int length, BrandProfile brandProfile) {
        RestTemplate restTemplate = new RestTemplate();

        // ✅ MIGLIORATO: Istruzioni specifiche per piattaforma
        String platformInstructions = getPlatformSpecificInstructions(platform, postType);
        
        // ✅ MIGLIORATO: Brand context più dettagliato
        String brandContext = buildBrandContext(brandProfile);
        
        // ✅ NUOVO: Definizione chiara delle tipologie di contenuto
        String contentTypesDefinition = getContentTypesDefinition();
        
        // ✅ MIGLIORATO: Prompt completo e strutturato
        String prompt = """
            %s
                        
            %s
                        
            %s
                        
            🎯 TRASFORMAZIONE CREATIVA:
            TESTIMONIANZA ORIGINALE: "%s"
                        
            OBIETTIVO: Trasforma questa testimonianza in contenuto MARKETING autentico che:
            • Racconti una STORIA con inizio/sviluppo/conclusione
            • Mostri il VALORE concreto del prodotto/servizio  
            • Includa ELEMENTI SPECIFICI della testimonianza originale
            • Sia SCRITTO IN PRIMA PERSONA dal brand
            • Crei CONNESSIONE EMOTIVA con il pubblico
            • Guidi verso una CALL-TO-ACTION chiara
                        
            📊 PARAMETRI CREATIVI APPLICATI:
            • Emozione: %d/100 → %s
            • Creatività: %d/100 → %s
            • Formalità: %d/100 → %s  
            • Urgenza: %d/100 → %s
            • Lunghezza: %d/100 → %s
                        
            🎪 FORMATO RISPOSTA OBBLIGATORIO (SOLO JSON):
            {
              "socialPostVersions": [
                "Testo completo versione 1 con struttura narrativa e hashtag",
                "Testo completo versione 2 con focus diverso e emoji appropriate",
                "Testo completo versione 3 con angolazione unica e call-to-action"
              ],
              "headlineVersions": [
                "Titolo accattivante max 8-10 parole versione 1",
                "Titolo benefit-driven max 8-10 parole versione 2", 
                "Titolo curiosity-gap max 8-10 parole versione 3"
              ],
              "shortQuoteVersions": [
                "Citazione potente ed estrapolabile max 15 parole versione 1",
                "Insight specifico del settore max 15 parole versione 2",
                "Dichiarazione memorabile max 15 parole versione 3"
              ],
              "callToActionVersions": [
                "CTA specifica e persuasiva versione 1",
                "CTA alternativa e coinvolgente versione 2",
                "CTA urgente e motivante versione 3"
              ]
            }
                        
            ⚠️ REGOLE STRETTE:
            • NESSUN testo generico o vago
            • NESSUN motto pubblicitario banale
            • SEMPRE contenuto specifico e contestualizzato
            • SEMPRE in prima persona dal punto di vista del brand
            • MASSIMA coerenza con l'identità del brand fornita
            """.formatted(
                brandContext,
                platformInstructions, 
                contentTypesDefinition,
                inputText,
                emotion, getEmotionDescription(emotion),
                creativity, getCreativityDescription(creativity),
                formality, getFormalityDescription(formality),
                urgency, getUrgencyDescription(urgency),
                length, getLengthDescription(length)
            );
        
        System.out.println("🎯 Prompt inviato a Groq:");
        System.out.println(prompt);
        System.out.println("======================");
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqApiKey);

        Map<String, Object> body = Map.of(
                "model", "llama-3.1-8b-instant",
                "messages", new Object[]{
                        Map.of("role", "system", "content", buildSystemPrompt()),
                        Map.of("role", "user", "content", prompt)
                },
                "temperature", calculateTemperature(creativity),
                "max_tokens", 1200, // ✅ Aumentato per contenuti più lunghi
                "response_format", Map.of("type", "json_object")
        );

        try {
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.exchange(GROQ_URL, HttpMethod.POST, request, Map.class);
            
            if (response.getBody() == null || !response.getBody().containsKey("choices")) {
                throw new RuntimeException("Risposta API vuota o malformata");
            }
            
            String content = (String) ((Map) ((Map) ((List<?>) response.getBody().get("choices")).get(0)).get("message")).get("content");
            
            // ✅ PULIZIA DELLA RISPOSTA
            content = cleanJsonResponse(content);
            System.out.println("📦 Risposta pulita: " + content);
            
            // ✅ PARSING E COSTRUZIONE DTO
            TestimonialDTO dto = parseResponseToDTO(content, inputText);
            System.out.println("✅ Contenuti generati con successo!");
            
            return dto;
            
        } catch (Exception e) {
            System.err.println("❌ Errore nella chiamata API: " + e.getMessage());
            return createFallbackDTO(inputText);
        }
    }
    
    // ✅ NUOVO: System prompt più dettagliato
    private String buildSystemPrompt() {
        return """
            Sei un COPYWRITER ESPERTO di social media marketing e content strategy.
            SPECIALIZZAZIONE: Trasformare testimonianze clienti in contenuti marketing autentici e coinvolgenti.
            
            COMPETENZE CHIAVE:
            • Storytelling persuasivo e strutturato
            • Adattamento del tono di voce al brand
            • Creazione di call-to-action efficaci
            • Ottimizzazione per diverse piattaforme social
            • Estrazione di insights value-driven dalle testimonianze
            
            OBIETTIVO PRIMARIO: Creare contenuti ORIGINALI che:
            - Raccontino una storia autentica
            - Mostrino valore concreto
            - Coinvolgano emotivamente
            - Guidino all'azione
            - Siano perfettamente allineati al brand
            """;
    }
    
    // ✅ MIGLIORATO: Istruzioni specifiche per piattaforma
    private String getPlatformSpecificInstructions(String platform, String postType) {
        String baseInstructions = switch (platform.toLowerCase()) {
            case "twitter", "x" -> """
                🐦 TWITTER/X SPECIFICO (max 280 caratteri):
                • Frasi brevi e ad alto impatto
                • 1-2 hashtag strategicamente posizionati
                • Linguaggio conciso e diretto
                • Call-to-action chiara e immediata
                • Formati preferiti: domanda, insight, statistica
                • Usa emoji con moderazione (max 2-3)""";
                
            case "instagram" -> """
                📸 INSTAGRAM SPECIFICO:
                • Caption emozionale con storytelling
                • 3-5 emoji strategicamente posizionate
                • 3-5 hashtag mirati e relevanti
                • Invito esplicito all'engagement (tagga, commenta, salva)
                • Tono autentico, personale e visivo
                • Struttura: hook + story + value + CTA""";
                
            case "linkedin" -> """
                💼 LINKEDIN SPECIFICO:
                • Approfondimento professionale e value-driven
                • Struttura: problema → soluzione → risultato → insight
                • Linguaggio settoriale appropriato ma accessibile
                • Call-to-action professionale e rilevante
                • Formato articolato ma scorrevole
                • Focus su insights e learnings professionali""";
                
            case "facebook" -> """
                📘 FACEBOOK SPECIFICO:
                • Tono conversazionale e community-oriented
                • Storytelling dettagliato ma non troppo lungo
                • Invito alla discussione nei commenti
                • 3-5 hashtag relevanti
                • Mix di valore emotivo e pratico""";
                
            default -> """
                🌐 CONTENUTO GENERICO PER SOCIAL MEDIA:
                • Tono adatto alla piattaforma %s
                • Bilanciamento tra valore emotivo e informativo
                • Call-to-action appropriata al contesto
                """.formatted(platform);
        };
        
        // ✅ Aggiungi istruzioni specifiche per il tipo di post se fornito
        if (postType != null && !postType.trim().isEmpty()) {
            String postTypeInstructions = getPostTypeInstructions(postType);
            baseInstructions += "\n\n" + postTypeInstructions;
        }
        
        return baseInstructions;
    }
    
    // ✅ NUOVO: Istruzioni per tipo specifico di post
    private String getPostTypeInstructions(String postType) {
        return switch (postType.toLowerCase()) {
            case "case study" -> """
                📊 CASE STUDY SPECIFICO:
                • Focus su dati e risultati misurabili
                • Struttura: sfida → approccio → risultati → key takeaways
                • Includi metriche concrete dove possibile""";
                
            case "testimonial" -> """
                💬 TESTIMONIAL SPECIFICO:
                • Enfatizza la trasformazione esperita
                • Mostra il viaggio da problema a soluzione
                • Highlight dei benefit più valued""";
                
            case "announcement" -> """
                📢 ANNUNCIO SPECIFICO:
                • Tonality excitement-oriented ma autentica
                • Chiara value proposition
                • Urgency naturale e non forzata""";
                
            default -> """
                📝 TIPO POST: %s
                • Adatta il formato al tipo di contenuto specificato
                """.formatted(postType);
        };
    }
    
    // ✅ MIGLIORATO: Brand context più ricco e strategico
    private String buildBrandContext(BrandProfile brand) {
        if (brand == null) {
            return """
                🏢 CONTESTO BRAND: Nessun profilo brand specificato.
                • Usa un tono generico professionale e autentico
                • Focus su value chiaro e storytelling coinvolgente
                """;
        }
        
        return String.format("""
            🏢 IDENTITÀ BRAND COMPLETA:
            NOME BRAND: %s
            TONO DI VOCE: %s (%s)
            VALORI FONDANTI: %s
            TARGET AUDIENCE: %s
            POSIZIONAMENTO: %s
            
            📝 LINEE GUIDA CONTENUTI:
            • PAROLE CHIAVE PREFERITE: %s
            • PAROLE DA EVITARE ASSOLUTAMENTE: %s
            • TAGLINE ISPIRAZIONALE: "%s"
            • HASHTAG BRAND: %s
            • CALL-TO-ACTION TIPICHE: %s
            
            🎬 STILE E COERENZA RICHIESTO:
            • SCRIVI IN PRIMA PERSONA COME SE FOSSI IL BRAND
            • MANTIENI ASSOLUTA COERENZA CON IL TONO %s
            • INCORPORA ALMENO 2-3 PAROLE CHIAVE PREFERITE
            • EVITA ASSOLUTAMENTE: %s
            • PARLA DIRETTAMENTE A: %s
            • TRASMETTI I VALORI: %s
            """,
            brand.getBrandName(),
            brand.getTone().toString(),
            getToneDescription(brand.getTone()),
            brand.getBrandValues(),
            brand.getTargetAudience(),
            brand.getPositioning() != null ? brand.getPositioning() : "Non specificato",
            String.join(", ", brand.getPreferredKeywords()),
            String.join(", ", brand.getAvoidedWords()),
            brand.getTagline(),
            String.join(" ", brand.getDefaultHashtags()),
            getBrandCTAs(brand),
            brand.getTone().toString(),
            String.join(", ", brand.getAvoidedWords()),
            brand.getTargetAudience(),
            brand.getBrandValues()
        );
    }
    
    // ✅ NUOVO: Definizione chiara delle tipologie di contenuto
    private String getContentTypesDefinition() {
        return """
            🎪 DEFINIZIONE TIPOLOGIE CONTENUTO:
            
            📱 SOCIAL POSTS (testo principale completo):
            • VERSIONE 1: Storytelling emozionale - focus sulla trasformazione
            • VERSIONE 2: Approccio value-driven - dati e risultati concreti  
            • VERSIONE 3: Angolazione unique - insight controintuitivo o sorprendente
            
            📰 HEADLINES (titoli accattivanti 8-10 parole):
            • VERSIONE 1: Curiosity-gap - crea desiderio di scoprire di più
            • VERSIONE 2: Benefit-driven - highlight del valore principale
            • VERSIONE 3: How-to/educativo - posiziona come soluzione
            
            💬 SHORT QUOTES (citazioni estrapolabili max 15 parole):
            • VERSIONE 1: Citazione ispirazionale - focus su mindset
            • VERSIONE 2: Insight settoriale - dimostra expertise
            • VERSIONE 3: Dichiarazione memorabile - facile da ricordare e condividere
            
            🎯 CALL-TO-ACTION (inviti all'azione specifici):
            • VERSIONE 1: CTA diretta e persuasiva
            • VERSIONE 2: CTA coinvolgente e community-oriented
            • VERSIONE 3: CTA urgente e motivante
            """;
    }
    
    // ✅ NUOVO: Helper methods per descrizioni parametriche
    private String getLengthDescription(int length) {
        if (length <= 20) return "MOLTO BREVE (max 100 caratteri) - Essenziale e impattante";
        if (length <= 40) return "BREVE (100-250 caratteri) - Conciso ma completo";
        if (length <= 60) return "MEDIO (250-500 caratteri) - Bilanciato e narrativo";
        if (length <= 80) return "LUNGO (500-800 caratteri) - Dettagliato e approfondito";
        return "MOLTO LUNGO (800+ caratteri) - Estremamente dettagliato";
    }
    
    private String getEmotionDescription(int emotion) {
        if (emotion <= 20) return "Tono neutro e razionale - Focus su logica e dati";
        if (emotion <= 40) return "Tono leggero e positivo - Approccio costruttivo";
        if (emotion <= 60) return "Tono empatico e coinvolgente - Bilanciato emozione/ragione";
        if (emotion <= 80) return "Tono emozionale e passionale - Forte coinvolgimento";
        return "Tono altamente emozionale - Massima carica emotiva";
    }
    
    private String getCreativityDescription(int creativity) {
        if (creativity <= 20) return "Approccio diretto e fattuale - Minimal creativity";
        if (creativity <= 40) return "Leggermente creativo - Small creative flourishes";
        if (creativity <= 60) return "Moderatamente creativo - Good balance creativity/clarity";
        if (creativity <= 80) return "Molto creativo - Strong creative elements";
        return "Estremamente creativo - Maximum innovation and originality";
    }
    
    private String getFormalityDescription(int formality) {
        if (formality <= 20) return "Tono informale e colloquiale - Linguaggio everyday";
        if (formality <= 40) return "Tono semi-informale - Conversazionale ma professionale";
        if (formality <= 60) return "Tono bilanciato - Professionale ma accessibile";
        if (formality <= 80) return "Tono formale - Linguaggio professionale strutturato";
        return "Tono molto formale - Linguaggio elevato e istituzionale";
    }
    
    private String getUrgencyDescription(int urgency) {
        if (urgency <= 20) return "Nessuna urgenza - Approccio contemplativo";
        if (urgency <= 40) return "Leggera urgenza - Gentle prompting";
        if (urgency <= 60) return "Urgenza moderata - Clear call-to-action";
        if (urgency <= 80) return "Alta urgenza - Strong prompting";
        return "Urgenza massima - Immediate action required";
    }
    
    // ✅ NUOVO: Descrizione estesa del tono
    private String getToneDescription(Object tone) {
        if (tone == null) return "Non specificato";
        
        return switch (tone.toString().toLowerCase()) {
            case "professional" -> "Autorevole ma accessibile, competente ma umano";
            case "friendly" -> "Caldo, accogliente, come un consiglio tra amici";
            case "authoritative" -> "Esperto, confidente, posizionamento leader";
            case "casual" -> "Informale, rilassato, linguaggio everyday";
            case "enthusiastic" -> "Energetico, positivo, carico di passione";
            case "empathetic" -> "Comprendente, supportivo, focalizzato sui bisogni";
            case "inspirational" -> "Motivante, elevante, focus su possibilità";
            case "humorous" -> "Leggero, spiritoso, approccio giocoso quando appropriato";
            default -> "Professionale e autentico";
        };
    }
    
    // ✅ NUOVO: Call-to-action tipiche del brand
    private String getBrandCTAs(BrandProfile brand) {
        if (brand.getPreferredCTAs() != null && !brand.getPreferredCTAs().isEmpty()) {
            return String.join(", ", brand.getPreferredCTAs());
        }
        return "Scopri di più, Inizia oggi, Unisciti a noi, Contattaci";
    }
    
    // ✅ NUOVO: Temperature dinamica basata sulla creatività
    private double calculateTemperature(int creativity) {
        return 0.7 + (creativity / 100.0 * 0.3); // Range: 0.7 - 1.0
    }
    
    // ✅ MIGLIORATO: Pulizia JSON più robusta
    private String cleanJsonResponse(String content) {
        if (content == null) return "{}";
        
        // Rimuovi markup code blocks
        content = content.replaceAll("(?i)```json", "").replaceAll("```", "").trim();
        
        // Rimuovi backtick singoli
        if (content.startsWith("`") && content.endsWith("`")) {
            content = content.substring(1, content.length() - 1);
        }
        
        // Sostituisci backtick residui con virgolette
        content = content.replace("`", "\"");
        
        // Fix per JSON malformati comuni
        content = content.replaceAll(",(\s*[}\\])])", "$1"); // trailing commas
        content = content.replaceAll("(\"[^\"]*\")?\\s*:\\s*'([^']*)'", "$1: \"$2\""); // single quotes to double
        
        return content;
    }
    
    // ✅ NUOVO: Parsing completo del response
    private TestimonialDTO parseResponseToDTO(String content, String inputText) {
        try {
            JsonNode root = mapper.readTree(content);
            
            TestimonialDTO dto = new TestimonialDTO();
            dto.setInputText(inputText);
            dto.setSocialPostVersions(extractList(root, "socialPostVersions"));
            dto.setHeadlineVersions(extractList(root, "headlineVersions"));
            dto.setShortQuoteVersions(extractList(root, "shortQuoteVersions"));
            dto.setCallToActionVersions(extractList(root, "callToActionVersions"));
            
            return dto;
            
        } catch (Exception e) {
            System.err.println("❌ Errore parsing JSON: " + e.getMessage());
            return createFallbackDTO(inputText);
        }
    }
    
    // ✅ MIGLIORATO: Extract list con ObjectMapper
    private List<String> extractList(JsonNode root, String key) {
        try {
            JsonNode arr = root.get(key);
            if (arr != null && arr.isArray()) {
                List<String> result = new ArrayList<>();
                for (JsonNode node : arr) {
                    if (node.isTextual()) {
                        result.add(node.asText());
                    }
                }
                // Assicura almeno 3 elementi
                while (result.size() < 3) {
                    result.add("Versione " + (result.size() + 1) + " - Generazione automatica");
                }
                return result;
            }
        } catch (Exception e) {
            System.err.println("⚠️ Errore estrazione lista per " + key + ": " + e.getMessage());
        }
        
        // Fallback
        return Arrays.asList(
            "Versione 1 - " + getFallbackText(key),
            "Versione 2 - " + getFallbackText(key),
            "Versione 3 - " + getFallbackText(key)
        );
    }
    
    // ✅ NUOVO: Testi di fallback contestuali
    private String getFallbackText(String key) {
        return switch (key) {
            case "socialPostVersions" -> "Contenuto social ottimizzato basato sulla testimonianza";
            case "headlineVersions" -> "Titolo accattivante per massimizzare engagement";
            case "shortQuoteVersions" -> "Citazione potente estrapolabile dalla testimonianza";
            case "callToActionVersions" -> "Invito all'azione efficace e persuasivo";
            default -> "Contenuto generato automaticamente";
        };
    }
    
    // ✅ NUOVO: DTO di fallback
    private TestimonialDTO createFallbackDTO(String inputText) {
        TestimonialDTO dto = new TestimonialDTO();
        dto.setInputText(inputText);
        dto.setSocialPostVersions(Arrays.asList(
            "Stiamo elaborando contenuti ottimali per la tua testimonianza...",
            "Generazione di social post coinvolgenti in corso...",
            "Preparazione contenuti social personalizzati..."
        ));
        dto.setHeadlineVersions(Arrays.asList(
            "Scopri come abbiamo trasformato questa esperienza",
            "Risultati straordinari dalla testimonianza del cliente", 
            "Storytelling autentico che ispira azione"
        ));
        dto.setShortQuoteVersions(Arrays.asList(
            "Trasformazione che ispira cambiamento",
            "Risultati che superano ogni aspettativa",
            "Esperienza che definisce nuovi standard"
        ));
        dto.setCallToActionVersions(Arrays.asList(
            "Scopri come possiamo aiutare anche te",
            "Inizia il tuo percorso di trasformazione oggi",
            "Contattaci per risultati eccezionali"
        ));
        return dto;
    }
    
    // ✅ RIMOSSO: Metodi deprecated (extract e extractListManual non più necessari)
}