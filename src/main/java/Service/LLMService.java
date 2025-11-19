package Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

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
import jakarta.annotation.PostConstruct;
import model.BrandProfile;

@Service
public class LLMService {
    
    @Value("${groq.api.key}")
    private String groqApiKey;
    
    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

    private final ObjectMapper mapper = new ObjectMapper();
    
    // ✅ GESTIONE CONCORRENZA E COSTI
    private final AtomicInteger activeRequests = new AtomicInteger(0);
    private static final int MAX_CONCURRENT_REQUESTS = 3;
    private final Queue<CompletableFuture<String>> requestQueue = new ConcurrentLinkedQueue<>();
    
    // ✅ MONITORAGGIO TOKEN
    private final AtomicLong tokensThisMinute = new AtomicLong(0);
    private final ScheduledExecutorService tokenMonitor = Executors.newScheduledThreadPool(1);
  
    @PostConstruct
    public void init() {
        tokenMonitor.scheduleAtFixedRate(() -> {
            long previous = tokensThisMinute.getAndSet(0);
            if (previous > 0) {
                System.out.println("🔄 Token contatore resettato. Precedente: " + previous);
            }
        }, 1, 1, TimeUnit.MINUTES);
    }

    public TestimonialDTO generate(String inputText, String platform, String postType,
            int emotion, int creativity, int formality, int urgency, int length, BrandProfile brandProfile) {
        
        if (!canProcessRequest(4000)) {
            throw new RuntimeException("Servizio occupato. Riprova tra qualche secondo.");
        }
        
        RestTemplate restTemplate = new RestTemplate();
        LengthConfig lengthConfig = calculateLengthConfig(length, platform);
        
        try {
            System.out.println("📏 " + platform + " - Lunghezza richiesta: " + length + "% → " + 
                             lengthConfig.getSocialPostLength() + " caratteri");
            
            // 🎯 GENERAZIONE CON MIGLIOR QUALITÀ
            String bestContent = generateBestQualityContent(inputText, platform, postType, 
                emotion, creativity, formality, urgency, lengthConfig, brandProfile, restTemplate);
            
            recordTokenUsage(4000);
            
            TestimonialDTO finalDTO = processFinalContent(bestContent, inputText, lengthConfig);
            
            System.out.println("✅ Generazione completata! Lunghezza target: " + lengthConfig.getSocialPostLength() + " caratteri");
            return finalDTO;
            
        } catch (Exception e) {
            System.err.println("❌ Errore generazione: " + e.getMessage());
            activeRequests.decrementAndGet();
            throw new RuntimeException("Errore nella generazione del contenuto: " + e.getMessage());
        }
    }

    // 🎯 METODO PRINCIPALE PER QUALITÀ OTTIMALE
    private String generateBestQualityContent(String inputText, String platform, String postType,
            int emotion, int creativity, int formality, int urgency, LengthConfig lengthConfig, 
            BrandProfile brandProfile, RestTemplate restTemplate) {
        
        int maxAttempts = 3;
        String bestContent = null;
        int bestScore = Integer.MAX_VALUE;
        
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            System.out.println("🔄 Tentativo " + attempt + "/" + maxAttempts + " per " + platform);
            
            try {
                String content = generateQualityContent(inputText, platform, postType, 
                    emotion, creativity, formality, urgency, lengthConfig, brandProfile, restTemplate, attempt);
                
                if (content == null) {
                    System.err.println("⚠️ Tentativo " + attempt + " fallito: contenuto nullo");
                    continue;
                }
                
                int qualityScore = evaluateQualityScore(content, lengthConfig);
                System.out.println("📊 Qualità tentativo " + attempt + ": " + qualityScore + "/100");
                
                if (qualityScore < bestScore) {
                    bestContent = content;
                    bestScore = qualityScore;
                    System.out.println("📈 Nuovo miglior risultato");
                }
                
                if (qualityScore <= 20) {
                    System.out.println("🎯 Qualità eccellente raggiunta");
                    return content;
                }
                
            } catch (Exception e) {
                System.err.println("⚠️ Tentativo " + attempt + " fallito: " + e.getMessage());
            }
        }
        
        System.out.println("🏆 Miglior qualità finale: " + bestScore + "/100");
        
        if (bestContent == null) {
            throw new RuntimeException("Impossibile generare contenuti di qualità dopo " + maxAttempts + " tentativi");
        }
        
        return bestContent;
    }

    // 🎯 VALUTAZIONE PUNTEGGIO QUALITÀ
    private int evaluateQualityScore(String content, LengthConfig config) {
        try {
            String cleanContent = content.replaceAll("(?i)```json", "").replaceAll("```", "").trim();
            JsonNode root = mapper.readTree(cleanContent);
            JsonNode contentNode = extractContentNode(root);
            
            int totalScore = 0;
            int factorCount = 0;
            
            // 🚨 VALUTAZIONE LUNGHEZZA RINFORZATA
            JsonNode socialPosts = contentNode.get("socialPostVersions");
            if (socialPosts != null && socialPosts.isArray()) {
                int lengthScore = 0;
                int postsEvaluated = 0;
                
                for (JsonNode post : socialPosts) {
                    String postText = post.asText();
                    int actualLength = postText.length();
                    int minRequired = (int)(config.getSocialPostLength() * 0.7); // 70% del target minimo
                    
                    if (actualLength < minRequired) {
                        // 🚨 PENALITÀ GRAVE per testi troppo corti
                        int penalty = 50 + ((minRequired - actualLength) * 2);
                        lengthScore += penalty;
                        System.out.println("🚨 Post troppo corto: " + actualLength + "/" + minRequired + " caratteri");
                    } else {
                        int deviation = Math.abs(actualLength - config.getSocialPostLength());
                        if (deviation > config.getSocialPostLength() * 0.15) {
                            lengthScore += (deviation * 100) / config.getSocialPostLength();
                        }
                    }
                    postsEvaluated++;
                }
                
                if (postsEvaluated > 0) {
                    totalScore += lengthScore / postsEvaluated;
                    factorCount++;
                }
            }
            
            int diversityScore = evaluateDiversityScore(contentNode);
            totalScore += diversityScore;
            factorCount++;
            
            return factorCount > 0 ? totalScore / factorCount : 100;
            
        } catch (Exception e) {
            return 100;
        }
    }

    // 🎯 VALUTAZIONE DIVERSITÀ
    private int evaluateDiversityScore(JsonNode contentNode) {
        int duplicateCount = 0;
        int totalItems = 0;
        
        JsonNode socialPosts = contentNode.get("socialPostVersions");
        if (socialPosts != null && socialPosts.isArray()) {
            Set<String> postBeginnings = new HashSet<>();
            for (JsonNode post : socialPosts) {
                String text = post.asText().trim();
                if (text.length() > 0) {
                    String beginning = text.substring(0, Math.min(40, text.length()));
                    if (!postBeginnings.add(beginning)) {
                        duplicateCount++;
                    }
                    totalItems++;
                }
            }
        }
        
        return duplicateCount > 0 ? 50 + (duplicateCount * 25) : 0;
    }

    // 🎯 GENERAZIONE CONTENUTO DI QUALITÀ - COMPLETAMENTE RIVISTA
    private String generateQualityContent(String inputText, String platform, String postType,
            int emotion, int creativity, int formality, int urgency, LengthConfig lengthConfig, 
            BrandProfile brandProfile, RestTemplate restTemplate, int attempt) {
        
        String platformGuidelines = getPlatformSpecificGuidelines(platform, lengthConfig);
        
        String prompt = """
            **SEI UN ESPERTO COPYWRITER PER MULTI-PIATTAFORMA - CREA CONTENUTI OTTIMIZZATI**
            
            ⚠️ **REGOLA FONDAMENTALE: RISPETTA LA LUNGHEZZA E IL FORMATO DELLA PIATTAFORMA**
            - LUNGHEZZA TARGET: %d caratteri (±15%%)
            - PIATTAFORMA: %s
            - %s
            
            **CONTESTO BRAND:**
            %s
            
            **TEMA DA SVILUPPARE:**
            "%s"
            
            🚫 **PROIBIZIONI ASSOLUTE:**
            - MAI usare prima persona ("io", "mi", "mio")
            - MAI generare testimonianze o recensioni
            - MAI creare contenuti troppo corti (<%d caratteri)
            - MAI usare linguaggio corporate generico
            
            ✅ **STRATEGIE PER CONTENUTI LUNGHI E QUALITATIVI:**
            
            1. **APPROCCIO STORYTELLING**
               - Racconta una trasformazione o un caso
               - Inizia con situazione "prima/dopo"
               - Includi dettagli specifici e concreti
            
            2. **APPROCCIO DATI E RISULTATI**  
               - Presenta statistiche sorprendenti
               - Mostra benefici misurabili
               - Includi metriche e KPI
            
            3. **APPROCCIO PROBLEMA-SOLUZIONE**
               - Identifica un pain point specifico
               - Presenta soluzione strutturata
               - Concludi con call-to-action chiara
            
            🔥 **ESEMPI DI CONTENUTI LUNGHI E VINCENTI:**
            
            ESEMPIO 1 (400+ caratteri):
            "Le aziende che investono in formazione digitale vedono un +45%% di produttività nei team. 
            Abbiamo aiutato 150+ organizzazioni a trasformare le competenze con programmi personalizzati. 
            I risultati? Riduzione del 60%% degli errori e aumento del 30%% della soddisfazione dei dipendenti.
            Qual è la skill più critica che la tua azienda sta sviluppando quest'anno?"
            
            ESEMPIO 2 (350+ caratteri):
            "Il 67%% dei progetti digitali fallisce per mancanza di una strategia chiara. 
            La nostra metodologia garantisce successo attraverso: fasi definite, metriche chiare, 
            e continuous improvement. I clienti hanno raggiunto ROI del 200%% in 12 mesi.
            Come misuri il successo dei tuoi progetti digitali?"
            
            📊 **PARAMETRI CREATIVI:**
            - Emozione: %d/100 (%s)
            - Creatività: %d/100 (%s)  
            - Formalità: %d/100 (%s)
            - Urgenza: %d/100 (%s)
            
            🎯 **RICHIESTA FINALE:**
            GENERA 3 VERSIONI COMPLETE, OGNUNA DI ALMENO %d CARATTERI, 
            ADATTATE ALLA PIATTAFORMA %s E AL TONO DEL BRAND.
            
            FORMATO OUTPUT JSON:
            {
              "socialPostVersions": [
                "Testo completo versione 1 con struttura dettagliata...",
                "Testo completo versione 2 con approccio diverso...", 
                "Testo completo versione 3 approfondito e coinvolgente..."
              ],
              "headlineVersions": [
                "Headline intrigante 1",
                "Headline intrigante 2", 
                "Headline intrigante 3"
              ],
              "shortQuoteVersions": [
                "Frase memorabile 1",
                "Frase memorabile 2",
                "Frase memorabile 3"
              ],
              "callToActionVersions": [
                "CTA specifica 1 →",
                "CTA specifica 2 →", 
                "CTA specifica 3 →"
              ]
            }
            """.formatted(
                lengthConfig.getSocialPostLength(),
                platform.toUpperCase(),
                platformGuidelines,
                buildCompactBrandContext(brandProfile),
                inputText.length() > 350 ? inputText.substring(0, 350) + "..." : inputText,
                (int)(lengthConfig.getSocialPostLength() * 0.7), // Minimo 70% del target
                emotion, getCompactEmotionDesc(emotion),
                creativity, getCompactCreativityDesc(creativity), 
                formality, getCompactFormalityDesc(formality),
                urgency, getCompactUrgencyDesc(urgency),
                (int)(lengthConfig.getSocialPostLength() * 0.8), // Enfatizza lunghezza minima
                platform.toUpperCase()
            );
        
        return callGroqAPI(prompt, restTemplate, 
            "Generazione " + platform + " (tentativo " + attempt + ")", 2500);
    }

    // 🎯 GUIDELINES SPECIFICHE PER PIATTAFORMA
    private String getPlatformSpecificGuidelines(String platform, LengthConfig config) {
        switch (platform.toUpperCase()) {
            case "LINKEDIN":
                return "FORMATO: Testo strutturato con paragrafi • ENGAGEMENT: Domande professionali • HASHTAG: 3-5 tematici";
            case "INSTAGRAM":
                return "FORMATO: Testo più visual con emoji • ENGAGEMENT: Domande emozionali • HASHTAG: 5-10 popolari";
            case "TWITTER":
                return "FORMATO: Testo conciso ma approfondito • ENGAGEMENT: Domande dirette • HASHTAG: 2-3 mirati";
            case "FACEBOOK":
                return "FORMATO: Testo conversazionale • ENGAGEMENT: Domande community • HASHTAG: 3-5 generali";
            default:
                return "FORMATO: Testo bilanciato • ENGAGEMENT: Domande coinvolgenti • HASHTAG: 3-5 relevanti";
        }
    }

    // 🏗️ CLASSE CONFIGURAZIONE LUNGHEZZA
    private static class LengthConfig {
        private final int socialPostLength;
        private final int headlineLength;
        private final int quoteLength;
        private final int ctaLength;
        private final String description;
        
        public LengthConfig(int socialPostLength, int headlineLength, int quoteLength, int ctaLength, String description) {
            this.socialPostLength = socialPostLength;
            this.headlineLength = headlineLength;
            this.quoteLength = quoteLength;
            this.ctaLength = ctaLength;
            this.description = description;
        }
        
        public int getSocialPostLength() { return socialPostLength; }
        public int getHeadlineLength() { return headlineLength; }
        public int getQuoteLength() { return quoteLength; }
        public int getCtaLength() { return ctaLength; }
        public String getDescription() { return description; }
    }

    // 📏 CALCOLO CONFIGURAZIONE LUNGHEZZA CON ADATTAMENTO PIATTAFORMA
    private LengthConfig calculateLengthConfig(int lengthPercentage, String platform) {
        // Base length per piattaforma
        int baseLength;
        switch (platform.toUpperCase()) {
            case "TWITTER":
                baseLength = 280; // Twitter ha limiti più stringenti
                break;
            case "INSTAGRAM":
                baseLength = 400; // Instagram permette testi più lunghi
                break;
            case "FACEBOOK":
                baseLength = 450; // Facebook ideale per contenuti medi
                break;
            case "LINKEDIN":
                baseLength = 500; // LinkedIn ottimale per contenuti lunghi
                break;
            default:
                baseLength = 400; // Default
        }
        
        // Applica la percentuale di lunghezza scelta dall'utente
        int adjustedLength = (int)(baseLength * (lengthPercentage / 100.0));
        
        // Assicurati lunghezze minime ragionevoli
        if (adjustedLength < 100) adjustedLength = 100;
        if (adjustedLength > 1300) adjustedLength = 1300;
        
        // Calcola lunghezze derivate
        int headlineLength = Math.max(30, adjustedLength / 10);
        int quoteLength = Math.max(40, adjustedLength / 8);
        int ctaLength = Math.max(20, adjustedLength / 15);
        
        String description = getLengthDescription(lengthPercentage);
        
        System.out.println("🎯 " + platform + " - Lunghezza calcolata: " + adjustedLength + 
                         " caratteri (base: " + baseLength + " * " + lengthPercentage + "%)");
        
        return new LengthConfig(adjustedLength, headlineLength, quoteLength, ctaLength, description);
    }

    private String getLengthDescription(int percentage) {
        if (percentage <= 15) return "Molto breve";
        if (percentage <= 35) return "Breve";
        if (percentage <= 55) return "Medio";
        if (percentage <= 75) return "Lungo";
        if (percentage <= 90) return "Molto lungo";
        return "Massimo";
    }

    // 🔧 PROCESSING CONTENUTO FINALE
    private TestimonialDTO processFinalContent(String optimizedContent, String inputText, LengthConfig lengthConfig) {
        try {
            String cleanContent = optimizedContent
                .replaceAll("(?i)```json", "")
                .replaceAll("```", "")
                .trim();
                
            JsonNode root = mapper.readTree(cleanContent);
            JsonNode contentNode = extractContentNode(root);
            
            TestimonialDTO dto = new TestimonialDTO();
            dto.setInputText(inputText);
            
            dto.setSocialPostVersions(extractList(contentNode, "socialPostVersions"));
            dto.setHeadlineVersions(extractList(contentNode, "headlineVersions"));
            dto.setShortQuoteVersions(extractList(contentNode, "shortQuoteVersions"));
            dto.setCallToActionVersions(extractList(contentNode, "callToActionVersions"));
            
            logGenerationResults(dto, lengthConfig, root);
            
            return dto;
            
        } catch (Exception e) {
            System.err.println("❌ Errore processing contenuto: " + e.getMessage());
            throw new RuntimeException("Errore nell'elaborazione del contenuto generato");
        }
    }

    // 📊 LOG RISULTATI GENERAZIONE
    private void logGenerationResults(TestimonialDTO dto, LengthConfig config, JsonNode root) {
        System.out.println("\n🎯 RISULTATI GENERAZIONE:");
        System.out.println("📐 Target di lunghezza: " + config.getSocialPostLength() + " caratteri");
        
        List<String> socialPosts = dto.getSocialPostVersions();
        for (int i = 0; i < socialPosts.size(); i++) {
            String post = socialPosts.get(i);
            int length = post.length();
            double deviation = ((double)(length - config.getSocialPostLength()) / config.getSocialPostLength()) * 100;
            String status = Math.abs(deviation) <= 15 ? "✅" : "⚠️";
            System.out.println(String.format("   %s Post %d: %d caratteri (%.1f%%)", status, i + 1, length, deviation));
        }
        
        checkAndLogDiversity(dto);
    }

    // 🔍 VERIFICA E LOG DIVERSITÀ
    private void checkAndLogDiversity(TestimonialDTO dto) {
        Set<String> socialPostBeginnings = new HashSet<>();
        for (String post : dto.getSocialPostVersions()) {
            if (post.length() > 0) {
                socialPostBeginnings.add(post.substring(0, Math.min(40, post.length())));
            }
        }
        
        boolean diverse = socialPostBeginnings.size() >= 2;
        System.out.println("🎭 Diversità: " + (diverse ? "✅" : "⚠️") + " (" + socialPostBeginnings.size() + "/3 distinte)");
    }

    // ✅ GESTIONE CONCORRENZA
    private boolean canProcessRequest(int estimatedTokens) {
        if (activeRequests.get() >= MAX_CONCURRENT_REQUESTS) {
            System.out.println("🚨 Limite concorrenza: " + activeRequests.get() + "/" + MAX_CONCURRENT_REQUESTS);
            return false;
        }
        
        long currentTokens = tokensThisMinute.get();
        long projectedTokens = currentTokens + estimatedTokens;
        
        if (projectedTokens > 28000) {
            System.out.println("🚨 Limite token: " + projectedTokens + "/30000 TPM");
            return false;
        }
        
        activeRequests.incrementAndGet();
        return true;
    }

    private void recordTokenUsage(int tokens) {
        long currentUsage = tokensThisMinute.addAndGet(tokens);
        activeRequests.decrementAndGet();
        
        double usagePercentage = (currentUsage / 30000.0) * 100;
        System.out.println("📊 Token usati: " + currentUsage + "/30000 (" + String.format("%.1f", usagePercentage) + "%)");
    }

    // 🔧 CHIAMATA API GROQ
    private String callGroqAPI(String prompt, RestTemplate restTemplate, String phase, int maxTokens) {
        System.out.println("🔄 " + phase + ": Invio richiesta...");
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqApiKey);

        Map<String, Object> body = Map.of(
            "model", "llama-3.1-8b-instant",
            "messages", new Object[]{
                Map.of("role", "system", "content", buildCompactSystemPrompt()),
                Map.of("role", "user", "content", prompt)
            },
            "temperature", 0.7,
            "max_tokens", maxTokens,
            "top_p", 0.9,
            "response_format", Map.of("type", "json_object")
        );

        try {
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.exchange(GROQ_URL, HttpMethod.POST, request, Map.class);
            
            if (response.getBody() == null || !response.getBody().containsKey("choices")) {
                throw new RuntimeException("Risposta API vuota");
            }
            
            String content = (String) ((Map) ((Map) ((List<?>) response.getBody().get("choices")).get(0)).get("message")).get("content");
            System.out.println("✅ " + phase + ": Successo - " + content.length() + " caratteri generati");
            
            return content;
            
        } catch (Exception e) {
            System.err.println("❌ Errore in " + phase + ": " + e.getMessage());
            activeRequests.decrementAndGet();
            throw new RuntimeException("Fallita fase: " + phase, e);
        }
    }

    // 🧠 SYSTEM PROMPT
    private String buildCompactSystemPrompt() {
        return """
            SEI UN COPYWRITER ESPERTO PER MULTI-PIATTAFORMA.
            
            REGOLE ASSOLUTE:
            ❌ MAI usare prima persona o generare testimonianze
            ❌ MAI creare contenuti troppo corti o generici
            ✅ SEMPRE rispettare lunghezza target e formato piattaforma
            ✅ SEMPRE creare contenuti dettagliati e strutturati
            
            OBIETTIVO: Generare contenuti social lunghi, dettagliati e ottimizzati per ogni piattaforma.
            
            FORMATO: Restituisci SEMPRE JSON valido.
            """;
    }

    // 🏗️ CONTESTO BRAND (invariato)
    private String buildCompactBrandContext(BrandProfile brand) {
        if (brand == null) return "Brand: Generico | Tono: Professionale | Target: Clienti generali";
        
        return String.format("""
            BRAND: %s
            DESCRIZIONE: %s
            VALORI: %s
            TONO: %s
            TARGET: %s
            KEYWORDS: %s
            """,
            brand.getBrandName(),
            brand.getBrandDescription() != null ? 
                (brand.getBrandDescription().length() > 100 ? 
                 brand.getBrandDescription().substring(0, 100) + "..." : brand.getBrandDescription()) 
                : "Non specificato",
            brand.getBrandValues() != null ? 
                (brand.getBrandValues().length() > 80 ? 
                 brand.getBrandValues().substring(0, 80) + "..." : brand.getBrandValues()) 
                : "Professionalità, Qualità",
            brand.getTone() != null ? brand.getTone().toString() : "Professionale",
            brand.getTargetAudience() != null ? 
                (brand.getTargetAudience().length() > 60 ? 
                 brand.getTargetAudience().substring(0, 60) + "..." : brand.getTargetAudience()) 
                : "Clienti generali",
            brand.getPreferredKeywords() != null ? 
                String.join(", ", brand.getPreferredKeywords().stream()
                    .limit(5)
                    .toList()) 
                : "qualità, innovazione, risultato"
        );
    }

    // 🎪 METODI DI SUPPORTO (invariati)
    private String getCompactEmotionDesc(int emotion) {
        if (emotion <= 20) return "Razionale";
        if (emotion <= 40) return "Positivo"; 
        if (emotion <= 60) return "Empatico";
        if (emotion <= 80) return "Passionale";
        return "Emozionale";
    }

    private String getCompactCreativityDesc(int creativity) {
        if (creativity <= 20) return "Strutturato";
        if (creativity <= 40) return "Innovativo"; 
        if (creativity <= 60) return "Creativo";
        if (creativity <= 80) return "Innovativo+";
        return "Estremamente creativo";
    }

    private String getCompactFormalityDesc(int formality) {
        if (formality <= 20) return "Informale";
        if (formality <= 40) return "Semi-informale";
        if (formality <= 60) return "Bilanciato";
        if (formality <= 80) return "Formale";
        return "Molto formale";
    }

    private String getCompactUrgencyDesc(int urgency) {
        if (urgency <= 20) return "Riflessivo";
        if (urgency <= 40) return "Suggerimento";
        if (urgency <= 60) return "Invito chiaro";
        if (urgency <= 80) return "Urgenza strategica";
        return "Urgenza massima";
    }

    private JsonNode extractContentNode(JsonNode root) {
        if (root.has("optimizedContent")) return root.get("optimizedContent");
        if (root.has("refinedContent")) return root.get("refinedContent");
        return root;
    }

    private List<String> extractList(JsonNode root, String key) {
        try {
            JsonNode arr = root.get(key);
            if (arr != null && arr.isArray()) {
                List<String> result = new ArrayList<>();
                for (JsonNode node : arr) {
                    if (node.isTextual()) {
                        String text = node.asText().trim();
                        if (!text.isEmpty()) {
                            result.add(text);
                        }
                    }
                }
                while (result.size() < 3) {
                    result.add("Versione " + (result.size() + 1) + " - Contenuto generato");
                }
                return result;
            }
        } catch (Exception e) {
            System.err.println("⚠️ Errore estrazione " + key + ": " + e.getMessage());
        }
        
        return Arrays.asList(
            "Esplora nuove possibilità con approccio innovativo",
            "Scopri come trasformare esperienza in risultati", 
            "Unisciti a percorso di eccellenza e innovazione"
        );
    }
}