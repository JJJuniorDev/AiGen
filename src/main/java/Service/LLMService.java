package Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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

        // 🎯 1. GENERAZIONE PRIMARIA dei post
        String initialContent = generateInitialPosts(inputText, platform, postType, 
            emotion, creativity, formality, urgency, length, brandProfile, restTemplate);
        
        // 🔄 2. AUTO-CORREZIONE: Groq ricontrolla e migliora i propri post
        String correctedContent = autoCorrectPosts(initialContent, inputText, brandProfile, platform, restTemplate);
        
        // 🎨 3. PROCESSING FINALE
        TestimonialDTO finalDTO = processFinalContent(correctedContent, inputText);
        
        System.out.println("✅ Generazione post completata con auto-correzione!");
        return finalDTO;
    }

 // 🎯 1. GENERAZIONE PRIMARIA - Crea post diversi tra loro
    private String generateInitialPosts(String inputText, String platform, String postType,
            int emotion, int creativity, int formality, int urgency, int length, 
            BrandProfile brandProfile, RestTemplate restTemplate) {
        
        String prompt = """
            %s
                    
            🎯 MISSIONE: Genera 3 POST SOCIAL DIVERSI tra loro per %s
                    
            💡 INPUT BASE DI PARTENZA:
            "%s"
                    
            🎪 OBIETTIVI CREATIVI:
            • Crea 3 POST COMPLETAMENTE DIVERSI nell'approccio e nell'angolazione
            • Ogni post deve avere una PERSONALITÀ e FOCUS unici
            • Usa STRUTTURE DIVERSE per ogni versione
            • Mantieni COERENZA con il brand ma ESPLORA angolazioni differenti
                    
            🎛️ PARAMETRI CREATIVI:
            • Emozione: %d/100 - %s
            • Creatività: %d/100 - %s
            • Formalità: %d/100 - %s
            • Piattaforma: %s - %s
                    
            📊 FORMATO RISPOSTA OBBLIGATORIO (SOLO JSON - STRUTTURA SEMPLICE):
            {
              "socialPostVersions": [
                "Testo completo del post 1 con approccio narrativo...",
                "Testo completo del post 2 con focus su risultati...", 
                "Testo completo del post 3 con angolazione emozionale..."
              ],
              "headlineVersions": [
                "Titolo breve e accattivante 1",
                "Titolo breve e accattivante 2", 
                "Titolo breve e accattivante 3"
              ],
              "shortQuoteVersions": [
                "Citazione breve e potente 1",
                "Citazione breve e potente 2",
                "Citazione breve e potente 3"
              ],
              "callToActionVersions": [
                "CTA diretta e persuasiva 1",
                "CTA coinvolgente 2",
                "CTA urgente 3"
              ]
            }
                    
            ⚡ REGOLE DIVERSITÀ:
            • VERSIONE 1: Approccio NARRATIVO - racconta una storia
            • VERSIONE 2: Approccio VALUE-DRIVEN - focus su benefici concreti  
            • VERSIONE 3: Approccio EMOTIVO - enfasi su sentimenti e trasformazione
            
            ❗❗❗ IMPORTANTE: 
            • Usa SEMPLICI ARRAY DI STRINGHE, non oggetti annidati
            • Ogni elemento deve essere una SINGOLA STRINGA
            • NESSUNA struttura complessa con "titolo", "testo", "immagine"
            • SOLO testo semplice negli array
                    
            🚫 STRUTTURA VIETATA:
            "socialPostVersions": [
              {
                "titolo": "...",   ← ERRATO!
                "testo": "...",    ← ERRATO!
                "immagine": "..."  ← ERRATO!
              }
            ]
                    
            ✅ STRUTTURA CORRETTA:
            "socialPostVersions": [
              "Testo completo del post qui...",  ← CORRETTO!
              "Altro testo completo qui...",     ← CORRETTO!
              "Terzo testo completo qui..."      ← CORRETTO!
            ]
            """.formatted(
                buildBrandContext(brandProfile),
                platform,
                inputText,
                emotion, getEmotionDescription(emotion),
                creativity, getCreativityDescription(creativity),
                formality, getFormalityDescription(formality),
                platform, getPlatformSpecificInstructions(platform, postType)
            );
        
        return callGroqAPI(prompt, restTemplate, "Generazione post iniziali");
    }

 // ✅ 2. AUTO-CORREZIONE: Groq corregge e migliora i propri post
    private String autoCorrectPosts(String initialContent, String originalInput, 
            BrandProfile brandProfile, String platform, RestTemplate restTemplate) {
        
        String correctionPrompt = """
            🔍 SISTEMA DI AUTO-CORREZIONE: Analizza e MIGLIORA questi post social generati.
            
            💡 INPUT ORIGINALE:
            "%s"
            
            📦 POST GENERATI DA CORREGGERE:
            %s
            
            🎯 CONTESTO:
            %s
            
            🔧 OBIETTIVI CORREZIONE:
            1. ✅ CORREGGI automaticamente errori grammaticali, ortografici, di sintassi
            2. ✅ MIGLIORA fluidità e naturalezza del linguaggio
            3. ✅ RAFFORZA diversità tra i post - devono essere ancora più diversi tra loro
            4. ✅ OTTIMIZZA per %s
            5. ✅ MANTIENI l'approccio narrativo dove presente ma rendilo più coinvolgente
            
            🎪 MIGLIORAMENTI RICHIESTI:
            • Assicura che i 3 post abbiano ANGOLAZIONI veramente diverse
            • Migliora le CALL-TO-ACTION per essere più persuasive
            • Rafforza la COERENZA con il brand
            • Correggi eventuali inconsistenze nel tono
            
            📊 FORMATO RISPOSTA OBBLIGATORIO (SOLO JSON - STRUTTURA SEMPLICE):
            {
              "correctedContent": {
                "socialPostVersions": ["Post corretto 1...", "Post corretto 2...", "Post corretto 3..."],
                "headlineVersions": ["Headline corretta 1...", "Headline corretta 2...", "Headline corretta 3..."],
                "shortQuoteVersions": ["Quote corretta 1...", "Quote corretta 2...", "Quote corretta 3..."],
                "callToActionVersions": ["CTA corretta 1...", "CTA corretta 2...", "CTA corretta 3..."]
              },
              "correctionsSummary": {
                "totalCorrections": 5,
                "mainImprovements": ["grammatica", "diversità", "persuasione"],
                "qualityScore": 92
              }
            }
            
            ⚠️ IMPORTANTE: 
            • Lascia che sia l'AI a correggere tutto - non fare correzioni manuali
            • USA SEMPRE ARRAY DI STRINGHE SEMPLICI, non oggetti annidati
            • Ogni elemento negli array deve essere una SINGOLA STRINGA
            """.formatted(
                originalInput,
                initialContent,
                buildBrandContext(brandProfile),
                platform
            );
        
        return callGroqAPI(correctionPrompt, restTemplate, "Auto-correzione post");
    }
    
    // 🎨 3. PROCESSING FINALE
    private TestimonialDTO processFinalContent(String correctedContent, String inputText) {
        try {
            String cleanContent = correctedContent
                .replaceAll("(?i)```json", "")
                .replaceAll("```", "")
                .trim();
                
            JsonNode root = mapper.readTree(cleanContent);
            
            // Estrae il contenuto corretto (che potrebbe essere nested in correctedContent)
            JsonNode contentNode = root;
            if (root.has("correctedContent")) {
                contentNode = root.get("correctedContent");
            }
            
            TestimonialDTO dto = new TestimonialDTO();
            dto.setInputText(inputText);
            dto.setSocialPostVersions(extractList(contentNode, "socialPostVersions"));
            dto.setHeadlineVersions(extractList(contentNode, "headlineVersions"));
            dto.setShortQuoteVersions(extractList(contentNode, "shortQuoteVersions"));
            dto.setCallToActionVersions(extractList(contentNode, "callToActionVersions"));
            
            // Log del miglioramento
            if (root.has("correctionsSummary")) {
                JsonNode summary = root.get("correctionsSummary");
                System.out.println("📈 Correzioni applicate: " + 
                    summary.get("totalCorrections").asInt() + " - Qualità: " + 
                    summary.get("qualityScore").asInt() + "/100");
            }
            
            return dto;
            
        } catch (Exception e) {
            System.err.println("❌ Errore processing finale: " + e.getMessage());
            return createFallbackDTO(inputText);
        }
    }

    // 🔧 METODO UNIFICATO per chiamate API
    private String callGroqAPI(String prompt, RestTemplate restTemplate, String phase) {
        System.out.println("🔄 " + phase + ": Invio richiesta a Groq...");
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqApiKey);

        Map<String, Object> body = Map.of(
            "model", "llama-3.1-8b-instant",
            "messages", new Object[]{
                Map.of("role", "system", "content", buildSystemPrompt()),
                Map.of("role", "user", "content", prompt)
            },
            "temperature", 0.8, // ✅ Più alto per più creatività e diversità
            "max_tokens", 2000,
            "response_format", Map.of("type", "json_object")
        );

        try {
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.exchange(GROQ_URL, HttpMethod.POST, request, Map.class);
            
            if (response.getBody() == null || !response.getBody().containsKey("choices")) {
                throw new RuntimeException("Risposta API vuota");
            }
            
            String content = (String) ((Map) ((Map) ((List<?>) response.getBody().get("choices")).get(0)).get("message")).get("content");
            System.out.println("✅ " + phase + ": Risposta ricevuta");
            
            return content;
            
        } catch (Exception e) {
            System.err.println("❌ Errore in " + phase + ": " + e.getMessage());
            throw new RuntimeException("Fallita fase: " + phase);
        }
    }

    // 🧠 SYSTEM PROMPT OTTIMIZZATO
    private String buildSystemPrompt() {
        return """
            Sei un ESPERTO CREATIVO di SOCIAL MEDIA specializzato in:
            • Generazione di POST SOCIAL DIVERSIFICATI e ORIGINALI
            • Auto-correzione e miglioramento qualità del contenuto
            • Adattamento multi-piattaforma e multi-format
            
            COMPETENZE CHIAVE:
            🎭 DIVERSITÀ CREATIVA: Crea contenuti con angolazioni e approcci diversi
            🔍 AUTO-ANALISI: Identifica e correggi errori nei contenuti generati
            📈 OTTIMIZZAZIONE: Migliora costantemente qualità e impatto
            🎯 PIATTAFORMA: Adatta contenuti specificamente per ogni social network
            
            PROCESSO CREATIVO:
            1. GENERA 3 versiones DIVERSE dello stesso concetto
            2. ASSICURA DIVERSITÀ reale nell'approccio e nel tono
            3. AUTO-CORREGGI errori e migliora la qualità
            4. OTTIMIZZA per la piattaforma specifica
            
            FORMATO: Restituisci SEMPRE JSON valido senza markdown.
            """;
    }

    // 🏗️ CONTESTO BRAND
    private String buildBrandContext(BrandProfile brand) {
        if (brand == null) return "Brand: Generico - Tono: Professionale - Target: Clienti potenziali";
        
        return String.format("""
            🏢 BRAND: %s
            🎭 TONO DI VOCE: %s
            👥 TARGET: %s
            💫 VALORI: %s
            🔑 PAROLE CHIAVE: %s
            ❌ DA EVITARE: %s
            """,
            brand.getBrandName(),
            brand.getTone().toString(),
            brand.getTargetAudience(),
            brand.getBrandValues(),
            String.join(", ", brand.getPreferredKeywords()),
            String.join(", ", brand.getAvoidedWords())
        );
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
            case "socialPostVersions" -> "Contenuto social ottimizzato";
            case "headlineVersions" -> "Titolo accattivante per massimizzare engagement";
            case "shortQuoteVersions" -> "Citazione potente estrapolabile";
            case "callToActionVersions" -> "Invito all'azione efficace e persuasivo";
            default -> "Contenuto generato automaticamente";
        };
    }
    
    // ✅ NUOVO: DTO di fallback
    private TestimonialDTO createFallbackDTO(String inputText) {
        TestimonialDTO dto = new TestimonialDTO();
        dto.setInputText(inputText);
        dto.setSocialPostVersions(Arrays.asList(
            "Stiamo elaborando contenuti ottimali...",
            "Generazione di social post coinvolgenti...",
            "Preparazione contenuti social personalizzati..."
        ));
        dto.setHeadlineVersions(Arrays.asList(
            "Scopri come abbiamo trasformato questa esperienza",
            "Risultati straordinari dal nostro lavoro", 
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

    // 🎪 DESCRIZIONI PARAMETRICHE
    private String getEmotionDescription(int emotion) {
        if (emotion <= 20) return "Neutro e razionale";
        if (emotion <= 40) return "Leggermente positivo"; 
        if (emotion <= 60) return "Empatico e coinvolgente";
        if (emotion <= 80) return "Emozionale e passionale";
        return "Altamente emozionale";
    }

    private String getCreativityDescription(int creativity) {
        if (creativity <= 20) return "Diretto e fattuale";
        if (creativity <= 40) return "Leggermente creativo";
        if (creativity <= 60) return "Moderatamente creativo";
        if (creativity <= 80) return "Molto creativo";
        return "Estremamente creativo";
    }

    private String getFormalityDescription(int formality) {
        if (formality <= 20) return "Informale e colloquiale";
        if (formality <= 40) return "Semi-informale";
        if (formality <= 60) return "Bilanciato";
        if (formality <= 80) return "Formale";
        return "Molto formale";
    }
    
    private String getUrgencyDescription(int urgency) {
        if (urgency <= 20) return "Nessuna urgenza - Approccio contemplativo";
        if (urgency <= 40) return "Leggera urgenza - Gentle prompting";
        if (urgency <= 60) return "Urgenza moderata - Clear call-to-action";
        if (urgency <= 80) return "Alta urgenza - Strong prompting";
        return "Urgenza massima - Immediate action required";
    }
    
    private String getLengthDescription(int length) {
        if (length <= 20) return "MOLTO BREVE (max 100 caratteri) - Essenziale e impattante";
        if (length <= 40) return "BREVE (100-250 caratteri) - Conciso ma completo";
        if (length <= 60) return "MEDIO (250-500 caratteri) - Bilanciato e narrativo";
        if (length <= 80) return "LUNGO (500-800 caratteri) - Dettagliato e approfondito";
        return "MOLTO LUNGO (800+ caratteri) - Estremamente dettagliato";
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
}