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
            int emotion, int creativity, int formality, int urgency, int length, BrandProfile brandProfile, String language) {
        
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
                emotion, creativity, formality, urgency, lengthConfig, brandProfile, restTemplate, language);
            
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
            BrandProfile brandProfile, RestTemplate restTemplate, String language) {
        
        int maxAttempts = 3;
        String bestContent = null;
        int bestScore = Integer.MAX_VALUE;
        
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            System.out.println("🔄 Tentativo " + attempt + "/" + maxAttempts + " per " + platform);
            
            try {
                String content = generateQualityContent(inputText, platform, postType, 
                    emotion, creativity, formality, urgency, lengthConfig, brandProfile, restTemplate, attempt, language);
                
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
            BrandProfile brandProfile, RestTemplate restTemplate, int attempt, String language) {
        
        String platformGuidelines = getPlatformSpecificGuidelines(platform, lengthConfig, language);
        
        String prompt = "it".equals(language) ? 
                buildItalianPrompt(inputText, platform, postType, emotion, creativity, formality, 
                                 urgency, lengthConfig, brandProfile, platformGuidelines, attempt) :
                buildEnglishPrompt(inputText, platform, postType, emotion, creativity, formality, 
                                 urgency, lengthConfig, brandProfile, platformGuidelines, attempt);
        
   
        
        return callGroqAPI(prompt, restTemplate, 
        	    "Generazione " + platform + " " + language.toUpperCase() + " (tentativo " + attempt + ")", 2500);
    }
    
    private String buildItalianPrompt(String inputText, String platform, String postType,
            int emotion, int creativity, int formality, int urgency, LengthConfig lengthConfig, 
            BrandProfile brandProfile, String platformGuidelines, int attempt) {
    	  return """
    		        **SEI UN ESPERTO COPYWRITER PER CONTENUTI BUSINESS PROFESSIONALI**
    		        
    		        🎯 **OBIETTIVO**: Creare contenuti autentici, credibili e utili per il target.
    		        
    		        🚫 **PROIBIZIONI ASSOLUTE - MAI VIOLARE**:
    		        1. ❌ MAI inventare statistiche, percentuali o dati numerici
    		        2. ❌ MAI usare testimonianze clienti o esperienze specifiche
    		        3. ❌ MAI fare claim medici, salutistici o risultati garantiti
    		        4. ❌ MAI usare linguaggio da vendita aggressiva
    		        
    		        ✅ **APPROCCI OBBLIGATORI**:
    		        
    		        **APPROCCIO 1: VALORE EDUCATIVO**
    		        - Spiega benefici in modo generale e oggettivo
    		        - Condividi conoscenze e best practices
    		        - Offri consigli pratici e insights
    		        - Focalizzati sul valore per il cliente
    		        
    		        **APPROCCIO 2: STORYTELLING AZIENDALE**
    		        - Racconta la filosofia e i valori del brand
    		        - Spiega l'approccio e la metodologia
    		        - Condividi la visione e la missione
    		        - Evidenzia l'esperienza e l'expertise
    		        
    		        **APPROCCIO 3: GUIDA PRATICA**  
    		        - Offri soluzioni a problemi comuni
    		        - Condividi framework e metodologie
    		        - Crea contenuti how-to e tutorial
    		        - Fornisce actionable insights
    		        
    		        📋 **CONTESTO BRAND**:
    		        %s
    		        
    		        🎨 **TEMA DA SVILUPPARE**:
    		        "%s"
    		        
    		        🔥 **ESEMPI CORRETTI - SEGUI QUESTI FORMATI**:
    		        
    		        ESEMPIO 1 (Yoga Aziendale):
    		        "La pratica regolare dello yoga in ambito aziendale può contribuire a migliorare il benessere psico-fisico dei collaboratori. Attraverso sessioni mirate di respirazione e movimento, è possibile favorire la concentrazione e ridurre lo stress accumulato durante la giornata lavorativa. Quali strategie adotti nella tua organizzazione per supportare l'equilibrio lavoro-vita privata?"
    		        
    		        ESEMPIO 2 (Consulenza Strategica):
    		        "Definire una strategia chiara è fondamentale per il successo di qualsiasi progetto imprenditoriale. Un approccio strutturato che includa obiettivi misurabili, analisi competitive e piano d'azione dettagliato può fare la differenza nel raggiungimento dei risultati attesi. Come affronti attualmente la pianificazione strategica nella tua attività?"
    		        
    		        ESEMPIO 3 (Prodotti Pet Food):
    		        "La scelta degli ingredienti nella formulazione di alimenti per animali domestici riveste un'importanza cruciale per il loro benessere a lungo termine. Privilegiare componenti di alta qualità e bilanciati dal punto di vista nutrizionale rappresenta un aspetto fondamentale della cura responsabile. Quali criteri consideri prioritari nella selezione dell'alimentazione per il tuo animale?"
    		        
    		        📊 **PARAMETRI CREATIVI**:
    		        - Emozione: %d/100 (%s)
    		        - Creatività: %d/100 (%s)
    		        - Formalità: %d/100 (%s)
    		        - Urgenza: %d/100 (%s)
    		        
    		        🎯 **RICHIESTA FINALE**:
    		        GENERA 3 VERSIONI COMPLETE E DIVERSE, OGNUNA DI %d-%d CARATTERI.
    		        USA APPROCCI DIVERSI PER OGNI VERSIONE E MANTIENI UN TONO PROFESSIONALE E CREDIBILE.
    		        
    		        FORMATO OUTPUT JSON:
    		        {
    		          "socialPostVersions": [
    		            "Testo versione 1 - Approccio educativo...",
    		            "Testo versione 2 - Approccio storytelling...",
    		            "Testo versione 3 - Approccio guida pratica..."
    		          ],
    		          "headlineVersions": [
    		            "Headline 1",
    		            "Headline 2", 
    		            "Headline 3"
    		          ],
    		          "shortQuoteVersions": [
    		            "Frase memorabile 1",
    		            "Frase memorabile 2",
    		            "Frase memorabile 3"
    		          ],
    		          "callToActionVersions": [
    		            "CTA 1 →",
    		            "CTA 2 →",
    		            "CTA 3 →"
    		          ]
    		        }
    		        """.formatted(
    		            buildCompactBrandContext(brandProfile, "it"),  // %s - CONTESTO BRAND
    		            inputText.length() > 300 ? inputText.substring(0, 300) + "..." : inputText,  // %s - TEMA
    		            emotion, getCompactEmotionDesc(emotion, "it"),  // %d, %s - EMOZIONE
    		            creativity, getCompactCreativityDesc(creativity, "it"),  // %d, %s - CREATIVITÀ
    		            formality, getCompactFormalityDesc(formality, "it"),  // %d, %s - FORMALITÀ
    		            urgency, getCompactUrgencyDesc(urgency, "it"),  // %d, %s - URGENZA
    		            (int)(lengthConfig.getSocialPostLength() * 0.8),  // %d - LUNGHEZZA MINIMA
    		            (int)(lengthConfig.getSocialPostLength() * 1.2)   // %d - LUNGHEZZA MASSIMA
    		        );
    		}
    
    private String buildEnglishPrompt(String inputText, String platform, String postType,
            int emotion, int creativity, int formality, int urgency, LengthConfig lengthConfig, 
            BrandProfile brandProfile, String platformGuidelines, int attempt) {
        
    	return """
    	        **YOU ARE AN EXPERT BUSINESS COPYWRITER FOR PROFESSIONAL CONTENT**
    	        
    	        🎯 **OBJECTIVE**: Create authentic, credible, and useful content for the target audience.
    	        
    	        🚫 **ABSOLUTE PROHIBITIONS - NEVER VIOLATE**:
    	        1. ❌ NEVER invent statistics, percentages, or numerical data
    	        2. ❌ NEVER use customer testimonials or specific experiences
    	        3. ❌ NEVER use first-person plural ("we", "our", "us")
    	        4. ❌ NEVER make medical, health, or guaranteed results claims
    	        5. ❌ NEVER use aggressive sales language
    	        
    	        ✅ **MANDATORY APPROACHES**:
    	        
    	        **APPROACH 1: EDUCATIONAL VALUE**
    	        - Explain benefits in general and objective terms
    	        - Share knowledge and best practices
    	        - Offer practical advice and insights
    	        - Focus on customer value
    	        
    	        **APPROACH 2: BRAND STORYTELLING**
    	        - Share the brand's philosophy and values
    	        - Explain the approach and methodology
    	        - Communicate vision and mission
    	        - Highlight experience and expertise
    	        
    	        **APPROACH 3: PRACTICAL GUIDE**
    	        - Offer solutions to common problems
    	        - Share frameworks and methodologies
    	        - Create how-to content and tutorials
    	        - Provide actionable insights
    	        
    	        📋 **BRAND CONTEXT**:
    	        %s
    	        
    	        🎨 **THEME TO DEVELOP**:
    	        "%s"
    	        
    	        🔥 **CORRECT EXAMPLES - FOLLOW THESE FORMATS**:
    	        
    	        EXAMPLE 1 (Corporate Yoga):
    	        "Regular yoga practice in corporate settings can contribute to improved employee well-being. Through targeted breathing and movement sessions, it's possible to enhance concentration and reduce accumulated work-related stress. What strategies does your organization implement to support work-life balance?"
    	        
    	        EXAMPLE 2 (Strategic Consulting):
    	        "Defining a clear strategy is essential for any business project's success. A structured approach including measurable objectives, competitive analysis, and detailed action plans can make a significant difference in achieving expected outcomes. How do you currently approach strategic planning in your business?"
    	        
    	        EXAMPLE 3 (Pet Food Products):
    	        "Ingredient selection in pet food formulation plays a crucial role in long-term animal well-being. Prioritizing high-quality, nutritionally balanced components represents a fundamental aspect of responsible pet care. What criteria do you consider most important when selecting nutrition for your pet?"
    	        
    	        📊 **CREATIVE PARAMETERS**:
    	        - Emotion: %d/100 (%s)
    	        - Creativity: %d/100 (%s)
    	        - Formality: %d/100 (%s)
    	        - Urgency: %d/100 (%s)
    	        
    	        🎯 **FINAL REQUEST**:
    	        GENERATE 3 COMPLETE AND DISTINCT VERSIONS, EACH %d-%d CHARACTERS.
    	        USE DIFFERENT APPROACHES FOR EACH VERSION AND MAINTAIN A PROFESSIONAL, CREDIBLE TONE.
    	        
    	        JSON OUTPUT FORMAT:
    	        {
    	          "socialPostVersions": [
    	            "Text version 1 - Educational approach...",
    	            "Text version 2 - Storytelling approach...",
    	            "Text version 3 - Practical guide approach..."
    	          ],
    	          "headlineVersions": [
    	            "Headline 1",
    	            "Headline 2",
    	            "Headline 3"
    	          ],
    	          "shortQuoteVersions": [
    	            "Memorable quote 1",
    	            "Memorable quote 2", 
    	            "Memorable quote 3"
    	          ],
    	          "callToActionVersions": [
    	            "CTA 1 →",
    	            "CTA 2 →",
    	            "CTA 3 →"
    	          ]
    	        }
    	        """.formatted(
    	            buildCompactBrandContext(brandProfile, "en"),  // %s - BRAND CONTEXT
    	            inputText.length() > 300 ? inputText.substring(0, 300) + "..." : inputText,  // %s - THEME
    	            emotion, getCompactEmotionDesc(emotion, "en"),  // %d, %s - EMOTION
    	            creativity, getCompactCreativityDesc(creativity, "en"),  // %d, %s - CREATIVITY
    	            formality, getCompactFormalityDesc(formality, "en"),  // %d, %s - FORMALITY
    	            urgency, getCompactUrgencyDesc(urgency, "en"),  // %d, %s - URGENCY
    	            (int)(lengthConfig.getSocialPostLength() * 0.8),  // %d - MIN LENGTH
    	            (int)(lengthConfig.getSocialPostLength() * 1.2)   // %d - MAX LENGTH
    	        );
    	}

    private String getPlatformSpecificGuidelines(String platform, LengthConfig config, String language) {
        if ("it".equals(language)) {
            switch (platform.toUpperCase()) {
                case "LINKEDIN": return "FORMATO: Testo strutturato con paragrafi • ENGAGEMENT: Domande professionali • HASHTAG: 3-5 tematici";
                case "INSTAGRAM": return "FORMATO: Testo più visual con emoji • ENGAGEMENT: Domande emozionali • HASHTAG: 5-10 popolari";
                case "TWITTER": return "FORMATO: Testo conciso ma approfondito • ENGAGEMENT: Domande dirette • HASHTAG: 2-3 mirati";
                default: return "FORMATO: Testo bilanciato • ENGAGEMENT: Domande coinvolgenti • HASHTAG: 3-5 relevanti";
            }
        } else {
            switch (platform.toUpperCase()) {
                case "LINKEDIN": return "FORMAT: Structured text with paragraphs • ENGAGEMENT: Professional questions • HASHTAG: 3-5 thematic";
                case "INSTAGRAM": return "FORMAT: More visual text with emojis • ENGAGEMENT: Emotional questions • HASHTAG: 5-10 popular";
                case "TWITTER": return "FORMAT: Concise but in-depth text • ENGAGEMENT: Direct questions • HASHTAG: 2-3 targeted";
                default: return "FORMAT: Balanced text • ENGAGEMENT: Engaging questions • HASHTAG: 3-5 relevant";
            }
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
            "model", "llama-3.3-70b-versatile",
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
    private String buildCompactBrandContext(BrandProfile brand, String language) {
        if (brand == null) {
            return "it".equals(language) ? 
                "Brand: Generico | Tono: Professionale | Target: Clienti generali" :
                "Brand: Generic | Tone: Professional | Target: General customers";
        }    
        if ("it".equals(language)) {
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
        } else {
            return String.format("""
                BRAND: %s
                DESCRIPTION: %s
                VALUES: %s
                TONE: %s
                TARGET: %s
                KEYWORDS: %s
                """,
                brand.getBrandName(),
                brand.getBrandDescription() != null ? 
                    (brand.getBrandDescription().length() > 100 ? 
                     brand.getBrandDescription().substring(0, 100) + "..." : brand.getBrandDescription()) 
                    : "Not specified",
                brand.getBrandValues() != null ? 
                    (brand.getBrandValues().length() > 80 ? 
                     brand.getBrandValues().substring(0, 80) + "..." : brand.getBrandValues()) 
                    : "Professionalism, Quality",
                brand.getTone() != null ? brand.getTone().toString() : "Professional",
                brand.getTargetAudience() != null ? 
                    (brand.getTargetAudience().length() > 60 ? 
                     brand.getTargetAudience().substring(0, 60) + "..." : brand.getTargetAudience()) 
                    : "General customers",
                brand.getPreferredKeywords() != null ? 
                    String.join(", ", brand.getPreferredKeywords().stream()
                        .limit(5)
                        .toList()) 
                    : "quality, innovation, results"
            );
        }
    }

    // 🎪 METODI DI SUPPORTO (invariati)
    private String getCompactEmotionDesc(int emotion, String language) {
    	 if ("it".equals(language)) {
    	        if (emotion <= 20) return "Razionale";
    	        if (emotion <= 40) return "Positivo"; 
    	        if (emotion <= 60) return "Empatico";
    	        if (emotion <= 80) return "Passionale";
    	        return "Emozionale";
    	    } else {
    	        if (emotion <= 20) return "Rational";
    	        if (emotion <= 40) return "Positive"; 
    	        if (emotion <= 60) return "Empathic";
    	        if (emotion <= 80) return "Passionate";
    	        return "Emotional";
    	    }
    }

    private String getCompactCreativityDesc(int creativity, String language) {
    	 if ("it".equals(language)) {
    	        if (creativity <= 20) return "Strutturato";
    	        if (creativity <= 40) return "Innovativo"; 
    	        if (creativity <= 60) return "Creativo";
    	        if (creativity <= 80) return "Innovativo+";
    	        return "Estremamente creativo";
    	    } else {
    	        if (creativity <= 20) return "Structured";
    	        if (creativity <= 40) return "Innovative"; 
    	        if (creativity <= 60) return "Creative";
    	        if (creativity <= 80) return "Highly Innovative";
    	        return "Extremely Creative";
    	    }
    }

    private String getCompactFormalityDesc(int formality, String language) {
    	if ("it".equals(language)) {
            if (formality <= 20) return "Informale";
            if (formality <= 40) return "Semi-informale";
            if (formality <= 60) return "Bilanciato";
            if (formality <= 80) return "Formale";
            return "Molto formale";
        } else {
            if (formality <= 20) return "Informal";
            if (formality <= 40) return "Semi-informal";
            if (formality <= 60) return "Balanced";
            if (formality <= 80) return "Formal";
            return "Very Formal";
        }
    }

    private String getCompactUrgencyDesc(int urgency, String language) {
    	 if ("it".equals(language)) {
    	        if (urgency <= 20) return "Riflessivo";
    	        if (urgency <= 40) return "Suggerimento";
    	        if (urgency <= 60) return "Invito chiaro";
    	        if (urgency <= 80) return "Urgenza strategica";
    	        return "Urgenza massima";
    	    } else {
    	        if (urgency <= 20) return "Reflective";
    	        if (urgency <= 40) return "Suggestion";
    	        if (urgency <= 60) return "Clear Invitation";
    	        if (urgency <= 80) return "Strategic Urgency";
    	        return "Maximum Urgency";
    	    }
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
    
    public String callGroqAPIForAnalysis(String prompt, String phase, int maxTokens) {
        System.out.println("🔄 " + phase + ": Invio richiesta analisi...");
        
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqApiKey);

        Map<String, Object> body = Map.of(
            "model", "llama-3.3-70b-versatile",
            "messages", new Object[]{
                Map.of("role", "system", "content", buildAnalysisSystemPrompt()),
                Map.of("role", "user", "content", prompt)
            },
            "temperature", 0.3, // Più basso per analisi più precise
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
            throw new RuntimeException("Fallita fase: " + phase, e);
        }
    }

    private String buildAnalysisSystemPrompt() {
        return """
            SEI UN ESPERTO DI ANALISI BRAND E MARKETING.
            
            OBIETTIVO: Analizzare contenuti di siti web e creare profili brand accurati.
            
            REGOLE:
            - Basati SOLO sul contenuto fornito
            - Sii preciso e oggettivo
            - Restituisci SEMPRE JSON valido
            - Non inventare informazioni non presenti nel contenuto
            
            FORMATO OUTPUT: JSON strutturato come richiesto.
            """;
    }
    
    
    
    
    public String callContentAssistantAPI(String prompt) {
        System.out.println("🤖 Content Assistant - Invio richiesta AI...");
        
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqApiKey);

        Map<String, Object> body = Map.of(
            "model", "llama-3.3-70b-versatile",
            "messages", new Object[]{
                Map.of("role", "system", "content", buildAssistantSystemPrompt()),
                Map.of("role", "user", "content", prompt)
            },
            "temperature", 0.3, // Basso per analisi più consistenti
            "max_tokens", 2000,
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
            System.out.println("✅ Content Assistant - Analisi completata");
            
            return content;
            
        } catch (Exception e) {
            System.err.println("❌ Errore Content Assistant API: " + e.getMessage());
            throw new RuntimeException("Fallita analisi assistant", e);
        }
    }

    private String buildAssistantSystemPrompt() {
        return """
            SEI UN ESPERTO ASSISTENTE DI CONTENT MARKETING.
            
            OBIETTIVO: Analizzare contenuti e fornire feedback costruttivi, specifici e azionabili.
            
            REGOLE ASSOLUTE:
            - Fornisci SOLO analisi basate sul contesto fornito
            - Sii costruttivo e professionale
            - Suggerimenti devono essere concreti e implementabili
            - Restituisci SEMPRE JSON valido nel formato richiesto
            - Non inventare informazioni non presenti nel contesto
            
            FOCUS: Qualità contenuto, allineamento brand, engagement potenziale, ottimizzazione piattaforma.
            """;
    } 
}