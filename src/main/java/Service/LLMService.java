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
    private static final int MAX_CONCURRENT_REQUESTS = 3; // Ridotto per sicurezza con 30K TPM
    private final Queue<CompletableFuture<String>> requestQueue = new ConcurrentLinkedQueue<>();
    
    // ✅ MONITORAGGIO TOKEN
    private final AtomicLong tokensThisMinute = new AtomicLong(0);
    private final ScheduledExecutorService tokenMonitor = Executors.newScheduledThreadPool(1);
    
   /* @PostConstruct
    public void init() {
        // Reset contatore token ogni minuto
        tokenMonitor.scheduleAtFixedRate(() -> {
            long previous = tokensThisMinute.getAndSet(0);
            if (previous > 0) {
                System.out.println("🔄 Contatore token resettato. Precedente: " + previous + " token");
            }
        }, 1, 1, TimeUnit.MINUTES);
    }
    
    public TestimonialDTO generate(String inputText, String platform, String postType,
            int emotion, int creativity, int formality, int urgency, int length, BrandProfile brandProfile) {
        
        // ✅ VERIFICA DISPONIBILITÀ PRIMA DI PROCEDERE
        if (!canProcessRequest(7000)) {
            throw new RuntimeException("Servizio temporaneamente occupato. Riprova tra qualche secondo.");
        }
        
        RestTemplate restTemplate = new RestTemplate();

        try {
            // 🎯 MANTIENI IL TUO SISTEMA A 3 FASI PREMIUM
            String initialContent = generateInitialPosts(inputText, platform, postType, 
                emotion, creativity, formality, urgency, length, brandProfile, restTemplate);
            
            String refinedContent = qualityRefinementLayer(initialContent, inputText, brandProfile, platform, restTemplate);
            
            String optimizedContent = finalOptimizationLayer(refinedContent, brandProfile, platform, postType, restTemplate);
            
            // ✅ REGISTRA UTILIZZO TOKEN
            recordTokenUsage(7000);
            
            TestimonialDTO finalDTO = processFinalContent(optimizedContent, inputText);
            
            System.out.println("✅ Generazione completata con sistema di raffinamento a 3 livelli!");
            return finalDTO;
            
        } catch (Exception e) {
            System.err.println("❌ Errore generazione: " + e.getMessage());
            // ✅ RILASCIA RISORSE IN CASO DI ERRORE
            activeRequests.decrementAndGet();
            return createEnhancedFallbackDTO(inputText);
        }
    }

    // ✅ GESTIONE CONCORRENZA
    private boolean canProcessRequest(int estimatedTokens) {
        // 1. Verifica limite concorrenza
        if (activeRequests.get() >= MAX_CONCURRENT_REQUESTS) {
            System.out.println("🚨 Limite concorrenza raggiunto: " + activeRequests.get() + "/" + MAX_CONCURRENT_REQUESTS);
            return false;
        }
        
        // 2. Verifica limite token/minuto
        long currentTokens = tokensThisMinute.get();
        long projectedTokens = currentTokens + estimatedTokens;
        
        if (projectedTokens > 28000) { // Margine sicurezza del 7%
            System.out.println("🚨 Limite token quasi raggiunto: " + projectedTokens + "/30000 TPM");
            return false;
        }
        
        activeRequests.incrementAndGet();
        System.out.println("📈 Richiesta accettata. Concorrenza: " + activeRequests.get() + "/" + MAX_CONCURRENT_REQUESTS + 
                          " | Token stimati: " + projectedTokens + "/30000");
        return true;
    }
    
    // ✅ REGISTRAZIONE UTILIZZO
    private void recordTokenUsage(int tokens) {
        long currentUsage = tokensThisMinute.addAndGet(tokens);
        activeRequests.decrementAndGet();
        
        double usagePercentage = (currentUsage / 30000.0) * 100;
        System.out.println("📊 Token utilizzati: " + currentUsage + "/30000 (" + String.format("%.1f", usagePercentage) + "%)");
        
        if (usagePercentage > 80) {
            System.out.println("⚠️  Attenzione: utilizzo token superiore all'80%");
        }
    }

    // 🎯 1. GENERAZIONE PRIMARIA - Sistema avanzato (TUO CODICE ORIGINALE)
    private String generateInitialPosts(String inputText, String platform, String postType,
            int emotion, int creativity, int formality, int urgency, int length, 
            BrandProfile brandProfile, RestTemplate restTemplate) {
        
        String prompt = """
            %s
                    
            🎯 MISSIONE CREATIVA: Genera 3 POST SOCIAL con APPROCCI DISTINTIVI per %s
                    
            💡 NUCLEO CREATIVO:
            "%s"
                    
            🎪 ARCHITETTURA CREATIVA:
            • VERSIONE 1: Approccio NARRATIVO-EMOZIONALE - Trasformazione e storytelling
            • VERSIONE 2: Approccio VALUE-PROPOSITION - Benefici concreti e risultati
            • VERSIONE 3: Approccio CONVERSATION-STARTER - Domanda provocatoria o insight unico
                    
            🎛️ PROFILO CREATIVO PERSONALIZZATO:
            • Emozione: %d/100 - %s
            • Creatività: %d/100 - %s
            • Formalità: %d/100 - %s
            • Urgenza: %d/100 - %s
            • Lunghezza: %d/100 - %s
                    
            📊 FORMATO RISPOSTA OBBLIGATORIO (JSON STRUTTURATO):
            {
              "socialPostVersions": [
                "Testo post narrativo-emozionale completo...",
                "Testo post value-proposition completo...", 
                "Testo post conversation-starter completo..."
              ],
              "headlineVersions": [
                "Headline narrativa coinvolgente",
                "Headline value-driven persuasiva", 
                "Headline provocatoria conversazionale"
              ],
              "shortQuoteVersions": [
                "Citazione emozionale memorabile",
                "Citazione value-focused incisiva",
                "Citazione insight-driven stimolante"
              ],
              "callToActionVersions": [
                "CTA emozionale e coinvolgente",
                "CTA value-oriented e persuasiva",
                "CTA conversazionale e interattiva"
              ]
            }
                    
            🚀 PRINCIPI CREATIVI APPLICATI:
            1. DIVERSITÀ STRUTTURALE: Ogni versione deve usare framework comunicativi diversi
            2. COERENZA CONTESTUALE: Mantenere allineamento con valori brand e piattaforma
            3. ORIGINALITÀ CONCEPTUALE: Evitare cliché, cercare angolazioni inedite
            4. IMPATTO EMOTIVO: Collegare al sentiment target audience appropriato
                    
            🎭 GUIDA ALLE ANGOLAZIONI:
            • NARRATIVO: Inizia con situazione, sviluppa trasformazione, conclude con insight
            • VALUE: Problema → Soluzione → Beneficio → Prova sociale → CTA
            • CONVERSAZIONALE: Domanda provocatoria → Dati sorprendenti → Invito al dialogo
            """.formatted(
                buildEnhancedBrandContext(brandProfile),
                platform.toUpperCase(),
                inputText,
                emotion, getEmotionDescription(emotion),
                creativity, getCreativityDescription(creativity),
                formality, getFormalityDescription(formality),
                urgency, getUrgencyDescription(urgency),
                length, getLengthDescription(length)
            );
        
        return callGroqAPI(prompt, restTemplate, "Generazione primaria avanzata");
    }

    // 🔍 2. LAYER DI RAFFINAMENTO QUALITATIVO (TUO CODICE ORIGINALE)
    private String qualityRefinementLayer(String initialContent, String originalInput, 
            BrandProfile brandProfile, String platform, RestTemplate restTemplate) {
        
        String refinementPrompt = """
            🎯 SISTEMA DI RAFFINAMENTO QUALITATIVO - ANALISI MULTIDIMENSIONALE
            
            📋 CONTENUTO DA RAFFINARE:
            %s
            
            🎪 CONTESTO COMPLETO:
            %s
            
            💡 INPUT ORIGINALE:
            "%s"
            
            🔧 DIMENSIONI DI RAFFINAMENTO:
            
            1. **COERENZA STRATEGICA**:
               - Allineamento con positioning brand?
               - Coerenza con tono di voce definito?
               - Aderenza ai valori core del brand?
            
            2. **EFFICACIA COMUNICATIVA**:
               - Chiarezza del messaggio centrale?
               - Persuasività delle call-to-action?
               - Appropriatezza del linguaggio per il target?
            
            3. **QUALITÀ CREATIVA**:
               - Originalità degli approcci?
               - Diversità reale tra le versioni?
               - Innovazione negli angoli narrativi?
            
            4. **OTTIMIZZAZIONE TECNICA**:
               - Adattamento ottimale alla piattaforma %s?
               - Lunghezza appropriata ai parametri?
               - Struttura che massimizza engagement?
            
            🛠️ AZIONI DI MIGLIORAMENTO RICHIESTE:
            • Rafforza la diversità concettuale tra le versioni
            • Migliora la fluidità narrativa e la progressione logica
            • Ottimizza le transizioni e il flow emozionale
            • Potenzia l'efficacia persuasiva mantenendo autenticità
            • Assicura massima coerenza con l'identità brand
            
            📊 FORMATO RISPOSTA:
            {
              "refinedContent": {
                "socialPostVersions": ["Versione raffinata 1...", "Versione raffinata 2...", "Versione raffinata 3..."],
                "headlineVersions": ["Headline raffinata 1...", "Headline raffinata 2...", "Headline raffinata 3..."],
                "shortQuoteVersions": ["Quote raffinata 1...", "Quote raffinata 2...", "Quote raffinata 3..."],
                "callToActionVersions": ["CTA raffinata 1...", "CTA raffinata 2...", "CTA raffinata 3..."]
              },
              "refinementMetrics": {
                "coherenceImprovement": 85,
                "creativityScore": 90,
                "platformOptimization": 88,
                "overallQuality": 92
              }
            }
            """.formatted(
                initialContent,
                buildEnhancedBrandContext(brandProfile),
                originalInput,
                platform
            );
        
        return callGroqAPI(refinementPrompt, restTemplate, "Raffinamento qualitativo");
    }

    // 🎨 3. LAYER DI OTTIMIZZAZIONE FINALE (TUO CODICE ORIGINALE)
    private String finalOptimizationLayer(String refinedContent, BrandProfile brandProfile, 
            String platform, String postType, RestTemplate restTemplate) {
        
        String optimizationPrompt = """
            ✨ OTTIMIZZAZIONE FINALE - ECCELLENZA ESPERIENZIALE
            
            📦 CONTENUTO DA PERFEZIONARE:
            %s
            
            🎯 CONTESTO BRAND AVANZATO:
            %s
            
            🚀 OBIETTIVI OTTIMIZZAZIONE:
            1. **ESPERIENZA LETTURA**:
               - Fluidità e ritmo narrativo ottimali
               - Transizioni naturali e coinvolgenti
               - Punteggiatura strategica per engagement
            
            2. **IMPATTO EMOTIVO**:
               - Potenziamento della connection emozionale
               - Rafforzamento dell'empatia con il target
               - Ottimizzazione del journey emozionale
            
            3. **PERSUASIONE AVANZATA**:
               - Call-to-action psicologicamente ottimizzate
               - Argumentation flow potenziato
               - Chiusure memorabili e action-oriented
            
            4. **ADATTAMENTO PLATAFORMA**:
               - Massima ottimizzazione per %s
               - Formattazione che sfrutta caratteristiche uniche
               - Timing e ritmo ideali per la piattaforma
            
            🎪 TIPO CONTENUTO: %s
            • Applica best practices specifiche per questo formato
            • Ottimizza struttura per massimizzare efficacia del tipo
            
            📊 FORMATO RISPOSTA:
            {
              "optimizedContent": {
                "socialPostVersions": ["Versione ottimizzata 1...", "Versione ottimizzata 2...", "Versione ottimizzata 3..."],
                "headlineVersions": ["Headline ottimizzata 1...", "Headline ottimizzata 2...", "Headline ottimizzata 3..."],
                "shortQuoteVersions": ["Quote ottimizzata 1...", "Quote ottimizzata 2...", "Quote ottimizzata 3..."],
                "callToActionVersions": ["CTA ottimizzata 1...", "CTA ottimizzata 2...", "CTA ottimizzata 3..."]
              },
              "optimizationResults": {
                "readabilityScore": 94,
                "emotionalImpact": 91,
                "persuasionPower": 89,
                "platformAlignment": 96
              }
            }
            """.formatted(
                refinedContent,
                buildEnhancedBrandContext(brandProfile),
                platform,
                postType != null ? postType : "Contenuto Social Generico"
            );
        
        return callGroqAPI(optimizationPrompt, restTemplate, "Ottimizzazione finale");
    }
    
    // 🔧 METODO API MIGLIORATO
    private String callGroqAPI(String prompt, RestTemplate restTemplate, String phase) {
        System.out.println("🔄 " + phase + ": Invio richiesta a Groq...");
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqApiKey);

        Map<String, Object> body = Map.of(
            "model", "llama-3.1-8b-instant",
            "messages", new Object[]{
                Map.of("role", "system", "content", buildEnhancedSystemPrompt()),
                Map.of("role", "user", "content", prompt)
            },
            "temperature", 0.7,
            "max_tokens", 2800, // Aumentato per contenuti più ricchi
            "top_p", 0.9,
            "response_format", Map.of("type", "json_object")
        );

        try {
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.exchange(GROQ_URL, HttpMethod.POST, request, Map.class);
            
            if (response.getBody() == null || !response.getBody().containsKey("choices")) {
                throw new RuntimeException("Risposta API vuota o malformata");
            }
            
            String content = (String) ((Map) ((Map) ((List<?>) response.getBody().get("choices")).get(0)).get("message")).get("content");
            System.out.println("✅ " + phase + ": Risposta ricevuta con successo");
            
            return content;
            
        } catch (Exception e) {
            System.err.println("❌ Errore in " + phase + ": " + e.getMessage());
            // ✅ RILASCIA SEMAFORO IN CASO DI ERRORE
            activeRequests.decrementAndGet();
            throw new RuntimeException("Fallita fase: " + phase, e);
        }
    }
    
    // 🎨 PROCESSING FINALE MIGLIORATO (IL RESTO DEL TUO CODICE RIMANE INVARIATO)
    private TestimonialDTO processFinalContent(String optimizedContent, String inputText) {
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
            
            logQualityMetrics(root);
            
            return dto;
            
        } catch (Exception e) {
            System.err.println("❌ Errore processing finale: " + e.getMessage());
            return createEnhancedFallbackDTO(inputText);
        }
    }

    // 🧠 SYSTEM PROMPT AVANZATO (MANTENUTO)
    private String buildEnhancedSystemPrompt() {
        return """
            Sei un ESPERTO DI COMUNICAZIONE STRATEGICA specializzato in:
            • Creazione di contenuti social multistrato e ad alto impatto
            • Ottimizzazione della user experience testuale
            • Adattamento avanzato multi-piattaforma
            • Raffinamento qualitativo iterativo

            COMPETENZE CORE:
            🎯 STRATEGIA CONTENUTI: Sviluppo di architetture comunicative sofisticate
            🔍 ANALISI QUALITATIVA: Identificazione e potenziamento di elementi critici
            📈 OTTIMIZZAZIONE: Miglioramento progressivo di tutti gli aspetti del contenuto
            🎭 NARRATIVA: Costruzione di journey emozionali e persuasivi

            PROCESSO OPERATIVO:
            1. GENERA contenuti con approcci distintivi e complementari
            2. ANALIZZA coerenza strategica ed efficacia comunicativa
            3. RAFFINA qualità tecnica e impatto emozionale
            4. OTTIMIZZA esperienza utente finale e performance

            PRINCIPI GUIDA:
            • Qualità sopra quantità - ogni parola deve avere uno scopo
            • Coerenza contestuale - allineamento totale con brand e piattaforma
            • Innovazione misurabile - creatività che serve obiettivi concreti
            • Esperienza-centric - focus sull'impatto sul pubblico target

            FORMATO: Restituisci SEMPRE JSON valido e strutturato.
            """;
    }

    // 🏗️ CONTESTO BRAND AVANZATO (MANTENUTO)
    private String buildEnhancedBrandContext(BrandProfile brand) {
        if (brand == null) return buildGenericBrandContext();
        
        return String.format("""
            🏢 ECOSISTEMA BRAND - CONTESTO COMPLETO:
            
            IDENTITÀ FONDAMENTALE:
            • NOME: %s
            • MISSION: %s
            • VALORI CORE: %s
            • POSIZIONAMENTO: %s
            
            ARCHITETTURA COMUNICAZIONE:
            • TONO DI VOCE: %s - %s
            • TARGET PRIMARIO: %s
            • PERSONALITÀ BRAND: %s
            
            ELEMENTI STRATEGICI:
            • PAROLE CHIAVE PREFERITE: %s
            • TERMINOLOGIA DA EVITARE: %s
            • CALL-TO-ACTION TIPICHE: %s
            • HASHTAG STRATEGICI: %s
            
            GUIDA APPLICATIVA:
            • APPROCCIO COMUNICAZIONE: %s
            • LIVELLO TECHNICALITY: %s
            • STILE NARRATIVO: %s
            """,
            brand.getBrandName(),
            brand.getBrandDescription(),
            brand.getBrandValues(),
            brand.getPositioning() != null ? brand.getPositioning() : "Non specificato",
            brand.getTone().toString(),
            getToneStrategicGuidance(brand.getTone()),
            brand.getTargetAudience(),
            getBrandPersonality(brand.getTone()),
            String.join(", ", brand.getPreferredKeywords()),
            String.join(", ", brand.getAvoidedWords()),
            getBrandCTAs(brand),
            brand.getDefaultHashtags() != null ? String.join(", ", brand.getDefaultHashtags()) : "Non specificati",
            getCommunicationApproach(brand.getTone()),
            getTechnicalityLevel(brand.getTone()),
            getNarrativeStyle(brand.getTone())
        );
    }

    // ✅ CONTESTO BRAND GENERICO (MANTENUTO)
    private String buildGenericBrandContext() {
        return """
            🏢 ECOSISTEMA BRAND - PROFILO GENERICO AVANZATO:
            
            IDENTITÀ FONDAMENTALE:
            • NOME: Brand Generico
            • MISSION: Fornire valore attraverso prodotti/servizi di qualità
            • VALORI CORE: Professionalità, Affidabilità, Innovazione
            • POSIZIONAMENTO: Soluzione affidabile nel proprio settore
            
            ARCHITETTURA COMUNICAZIONE:
            • TONO DI VOCE: Professionale e Accessibile
            • TARGET PRIMARIO: Clienti potenziali informati
            • PERSONALITÀ BRAND: Esperto affidabile e approachable
            
            ELEMENTI STRATEGICI:
            • PAROLE CHIAVE PREFERITE: qualità, risultato, esperienza, valore
            • TERMINOLOGIA DA EVITARE: termini tecnici eccessivi, gergo settoriale
            • CALL-TO-ACTION TIPICHE: Scopri di più, Inizia oggi, Contattaci
            • HASHTAG STRATEGICI: #innovazione #qualità #risultati
            
            GUIDA APPLICATIVA:
            • APPROCCIO COMUNICAZIONE: Bilanciato tra autorevolezza e accessibilità
            • LIVELLO TECHNICALITY: Medio - tecnicismo moderato quando rilevante
            • STILE NARRATIVO: Strutturato ma coinvolgente
            """;
    }

    // ... (MANTIENI TUTTI GLI ALTRI METODI DI SUPPORTO ESISTENTI)
    // extractContentNode, logQualityMetrics, createEnhancedFallbackDTO,
    // getEmotionDescription, getCreativityDescription, getFormalityDescription,
    // getUrgencyDescription, getLengthDescription, getToneStrategicGuidance,
    // getBrandPersonality, getCommunicationApproach, getTechnicalityLevel,
    // getNarrativeStyle, getBrandCTAs, extractList

    private JsonNode extractContentNode(JsonNode root) {
        if (root.has("optimizedContent")) return root.get("optimizedContent");
        if (root.has("refinedContent")) return root.get("refinedContent");
        if (root.has("correctedContent")) return root.get("correctedContent");
        return root;
    }

    private void logQualityMetrics(JsonNode root) {
        if (root.has("optimizationResults")) {
            JsonNode metrics = root.get("optimizationResults");
            System.out.println("📊 Metriche Ottimizzazione Finale:");
            System.out.println("   • Leggibilità: " + metrics.get("readabilityScore").asInt() + "/100");
            System.out.println("   • Impatto Emotivo: " + metrics.get("emotionalImpact").asInt() + "/100");
            System.out.println("   • Potere Persuasivo: " + metrics.get("persuasionPower").asInt() + "/100");
            System.out.println("   • Allineamento Piattaforma: " + metrics.get("platformAlignment").asInt() + "/100");
        } else if (root.has("refinementMetrics")) {
            JsonNode metrics = root.get("refinementMetrics");
            System.out.println("📈 Metriche Raffinamento:");
            System.out.println("   • Coerenza: " + metrics.get("coherenceImprovement").asInt() + "/100");
            System.out.println("   • Creatività: " + metrics.get("creativityScore").asInt() + "/100");
            System.out.println("   • Qualità Complessiva: " + metrics.get("overallQuality").asInt() + "/100");
        }
    }

    private TestimonialDTO createEnhancedFallbackDTO(String inputText) {
        TestimonialDTO dto = new TestimonialDTO();
        dto.setInputText(inputText);
        dto.setSocialPostVersions(Arrays.asList(
            "Scopri come stiamo rivoluzionando l'esperienza attraverso innovazione e qualità...",
            "Risultati eccezionali nascono da approcci straordinari - ecco la nostra vision...",
            "Immagina un percorso di trasformazione che supera ogni aspettativa. Inizia qui..."
        ));
        dto.setHeadlineVersions(Arrays.asList(
            "Innovazione che trasforma esperienze in risultati straordinari",
            "Dall'idea alla eccellenza: il percorso che definisce nuovi standard", 
            "Oltre le aspettative: quando la qualità incontra l'innovazione"
        ));
        dto.setShortQuoteVersions(Arrays.asList(
            "Trasformazione non è un destino, ma una scelta consapevole",
            "I risultati più straordinari nascono da approcci fuori dall'ordinario",
            "L'innovazione vera trasforma non solo cosa facciamo, ma come pensiamo"
        ));
        dto.setCallToActionVersions(Arrays.asList(
            "Inizia il tuo viaggio verso risultati straordinari - Scopri come",
            "Pronto a trasformare la tua esperienza? Il primo passo inizia qui",
            "Unisciti a chi già vede risultati eccezionali - Contattaci oggi"
        ));
        return dto;
    }

    private String getEmotionDescription(int emotion) {
        if (emotion <= 20) return "Razionale e analitico - Focus su logica e dati";
        if (emotion <= 40) return "Positivo e ottimista - Linguaggio costruttivo e forward-looking"; 
        if (emotion <= 60) return "Empatico e coinvolgente - Balance tra cuore e ragione";
        if (emotion <= 80) return "Passionale e intenso - Enfasi su sentimenti e trasformazione";
        return "Altamente emozionale - Comunicazione viscerale e memorabile";
    }

    private String getCreativityDescription(int creativity) {
        if (creativity <= 20) return "Strutturato e predittibile - Approccio lineare e consolidato";
        if (creativity <= 40) return "Innovativo moderato - Evoluzione di concept esistenti";
        if (creativity <= 60) return "Creativo e originale - Angolazioni inedite e fresh perspectives";
        if (creativity <= 80) return "Altamente innovativo - Breakthrough thinking e approcci disruptivi";
        return "Estremamente creativo - Visionary concepts e paradigma-shifting";
    }

    private String getFormalityDescription(int formality) {
        if (formality <= 20) return "Informale e colloquiale - Linguaggio everyday e conversazionale";
        if (formality <= 40) return "Semi-informale - Professionale ma approachable e relazionale";
        if (formality <= 60) return "Bilanciato - Mix ottimale tra autorevolezza e accessibilità";
        if (formality <= 80) return "Formale - Linguaggio strutturato e professional-oriented";
        return "Molto formale - Comunicazione istituzionale e high-authority";
    }
    
    private String getUrgencyDescription(int urgency) {
        if (urgency <= 20) return "Contemplativo - Approccio riflessivo e considerato";
        if (urgency <= 40) return "Gentle prompting - Suggerimento soft e non pressante";
        if (urgency <= 60) return "Call-to-action chiara - Invito definito ma non aggressivo";
        if (urgency <= 80) return "Urgenza strategica - Prompting forte con rationale chiaro";
        return "Urgenza massima - Comunicazione time-sensitive e action-critical";
    }
    
    private String getLengthDescription(int length) {
        if (length <= 20) return "Micro-content (max 100 caratteri) - Essenziale e high-impact";
        if (length <= 40) return "Breve e conciso (100-250) - Messaggio focalizzato e diretto";
        if (length <= 60) return "Medio bilanciato (250-500) - Narrativa completa ma snella";
        if (length <= 80) return "Approfondito (500-800) - Storytelling dettagliato e articolato";
        return "Esteso e completo (800+) - Comunicazione comprehensive e exhaustive";
    }

    private String getToneStrategicGuidance(Object tone) {
        return switch (tone.toString().toLowerCase()) {
            case "professional" -> "Autorevolezza bilanciata con accessibilità - Competenza che ispira fiducia";
            case "friendly" -> "Calore genuino e approccio relazionale - Come consigliare un collega stimato";
            case "authoritative" -> "Leadership pensante e visione chiara - Guida esperta che ispira followership";
            case "casual" -> "Autenticità everyday e linguaggio reale - Comunicazione tra pari";
            case "enthusiastic" -> "Energia contagiosa e ottimismo motivante - Passione che ispira azione";
            case "empathetic" -> "Ascolto profondo e comprensione genuina - Supporto che costruisce relazione";
            case "inspirational" -> "Visione elevante e purpose-driven - Comunicazione che mobilita verso higher goals";
            case "humorous" -> "Levità intelligente e approccio giocoso - Serious message delivered with smile";
            default -> "Comunicazione strategicamente allineata agli obiettivi brand";
        };
    }

    private String getBrandPersonality(Object tone) {
        return switch (tone.toString().toLowerCase()) {
            case "professional" -> "L'Esperto Affidabile";
            case "friendly" -> "Il Consigliere di Fiducia"; 
            case "authoritative" -> "Il Leader Visionario";
            case "casual" -> "Il Compagno Autentico";
            case "enthusiastic" -> "L'Evangelista Appassionato";
            case "empathetic" -> "L'Ascoltatore Comprensivo";
            case "inspirational" -> "Il Motivatore Trasformativo";
            case "humorous" -> "Il Comunicatore Spiritoso";
            default -> "Personalità Brand Allineata";
        };
    }

    private String getCommunicationApproach(Object tone) {
        return switch (tone.toString().toLowerCase()) {
            case "professional" -> "Evidence-based e risultato-oriented";
            case "friendly" -> "Relazionale e collaborative";
            case "authoritative" -> "Vision-setting e direction-giving";
            case "casual" -> "Conversational e peer-to-peer";
            case "enthusiastic" -> "Energy-building e action-motivating";
            case "empathetic" -> "Need-understanding e solution-focusing";
            case "inspirational" -> "Purpose-elevating e potential-unlocking";
            case "humorous" -> "Engagement-driving e memorability-creating";
            default -> "Strategicamente allineato agli obiettivi";
        };
    }

    private String getTechnicalityLevel(Object tone) {
        return switch (tone.toString().toLowerCase()) {
            case "professional", "authoritative" -> "Alto - Tecnicismo quando aggiunge valore";
            case "friendly", "casual", "humorous" -> "Basso - Linguaggio accessibile e universale";
            case "enthusiastic", "inspirational" -> "Medio - Concetti complessi resi semplici";
            case "empathetic" -> "Personalizzato - Adattato al livello dell'interlocutore";
            default -> "Bilanciato in base al contesto";
        };
    }

    private String getNarrativeStyle(Object tone) {
        return switch (tone.toString().toLowerCase()) {
            case "professional" -> "Strutturato e progressione logica";
            case "friendly" -> "Conversazionale e aneddotico";
            case "authoritative" -> "Dichiarativo e vision-setting";
            case "casual" -> "Stream-of-consciousness e naturale";
            case "enthusiastic" -> "Energetico e climax-building";
            case "empathetic" -> "Reflective e journey-focused";
            case "inspirational" -> "Elevating e transformation-narrating";
            case "humorous" -> "Punchline-driven e surprise-incorporating";
            default -> "Adattivo al contesto comunicativo";
        };
    }

    private String getBrandCTAs(BrandProfile brand) {
        if (brand.getPreferredCTAs() != null && !brand.getPreferredCTAs().isEmpty()) {
            return String.join(", ", brand.getPreferredCTAs());
        }
        return "Scopri di più, Inizia il percorso, Unisciti alla community, Contattaci per approfondire";
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
                    result.add("Versione " + (result.size() + 1) + " - Contenuto ottimizzato in fase di generazione");
                }
                return result;
            }
        } catch (Exception e) {
            System.err.println("⚠️ Errore estrazione lista per " + key + ": " + e.getMessage());
        }
        
        return Arrays.asList(
            "Esplora nuove possibilità con il nostro approccio innovativo",
            "Scopri come trasformare la tua esperienza in risultati straordinari", 
            "Unisciti a chi già vive un percorso di eccellenza e innovazione"
        );
    } */
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
        LengthConfig lengthConfig = calculateLengthConfig(length);
        
        try {
            System.out.println("📏 Lunghezza richiesta: " + length + "% → " + lengthConfig.getSocialPostLength() + " caratteri (Social Post)");
            
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
            System.out.println("🔄 Tentativo " + attempt + "/" + maxAttempts + " per qualità ottimale");
            
            try {
                // 🎯 GENERAZIONE CON ENFASI SU QUALITÀ
                String content = generateQualityContent(inputText, platform, postType, 
                    emotion, creativity, formality, urgency, lengthConfig, brandProfile, restTemplate, attempt);
                
                // 🎯 VALUTAZIONE QUALITÀ
                int qualityScore = evaluateQualityScore(content, lengthConfig);
                
                System.out.println("📊 Qualità tentativo " + attempt + ": " + qualityScore + "/100");
                
                if (qualityScore < bestScore) {
                    bestContent = content;
                    bestScore = qualityScore;
                    System.out.println("📈 Nuovo miglior risultato");
                }
                
                // 🎯 ACCETTA SE QUALITÀ ECCELLENTE
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
            throw new RuntimeException("Impossibile generare contenuti di qualità accettabile dopo " + maxAttempts + " tentativi");
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
            
            // 🎯 VALUTAZIONE LUNGHEZZA SOCIAL POSTS
            JsonNode socialPosts = contentNode.get("socialPostVersions");
            if (socialPosts != null && socialPosts.isArray()) {
                int lengthScore = 0;
                for (JsonNode post : socialPosts) {
                    int actualLength = post.asText().length();
                    int deviation = Math.abs(actualLength - config.getSocialPostLength());
                    // Penalità progressiva per deviazioni >10%
                    if (deviation > config.getSocialPostLength() * 0.1) {
                        lengthScore += (deviation * 100) / config.getSocialPostLength();
                    }
                }
                totalScore += lengthScore / socialPosts.size();
                factorCount++;
            }
            
            // 🎯 VALUTAZIONE DIVERSITÀ
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
        
        // Verifica Social Posts
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
        
        // Verifica Quotes
        JsonNode quotes = contentNode.get("shortQuoteVersions");
        if (quotes != null && quotes.isArray()) {
            Set<String> quoteSet = new HashSet<>();
            for (JsonNode quote : quotes) {
                String text = quote.asText().trim();
                if (text.length() > 0) {
                    if (!quoteSet.add(text)) {
                        duplicateCount++;
                    }
                    totalItems++;
                }
            }
        }
        
        // Calcola punteggio diversità (0 = perfetto, 100 = pessimo)
        return duplicateCount > 0 ? 50 + (duplicateCount * 25) : 0;
    }

    // 🎯 GENERAZIONE CONTENUTO DI QUALITÀ
    private String generateQualityContent(String inputText, String platform, String postType,
            int emotion, int creativity, int formality, int urgency, LengthConfig lengthConfig, 
            BrandProfile brandProfile, RestTemplate restTemplate, int attempt) {
        
        String prompt = """
            **CREAZIONE CONTENUTI SOCIAL DI ALTA QUALITÀ**
            
            CONTESTO BRAND:
            %s
            
            INPUT UTENTE:
            %s
            
             REGOLE IMPORTANTI:
  - NON scrivere mai contenuti in prima persona singolare ("io")
  - Evita frasi come:
      "la mia vita è cambiata", "sono felice che", "grazie a...", "mi sento più", "ho migliorato"
  - Scrivi come un copywriter esterno che promuove il brand, non come un cliente
  
            SPECIFICHE TECNICHE:
            - Piattaforma: %s
            - Tipo contenuto: %s
            - Lunghezza Social Posts: %d caratteri (±10%%)
            - Lunghezza Headlines: %d caratteri (±20%%)
            - Lunghezza Quotes: %d caratteri (±20%%)
            - Lunghezza CTAs: %d caratteri (±20%%)
            
            PARAMETRI CREATIVI:
            - Emozione: %d/100 (%s) - influenza tono e linguaggio
            - Creatività: %d/100 (%s) - influenza originalità e approcci
            - Formalità: %d/100 (%s) - influenza stile comunicativo
            - Urgenza: %d/100 (%s) - influenza call-to-action
            
            **REQUISITI DI QUALITÀ:**
            
            1. LUNGHEZZA PRECISA:
               - Conta i caratteri di ogni social post
               - Rispetta i range specificati
               - Espandi se troppo corto, riduci se troppo lungo
            
            2. DIVERSITÀ OBBLIGATORIA:
               - 3 VERSIONI COMPLETAMENTE DIVERSE
               - Approcci distinti per ogni social post:
                 • VERSIONE 1: Narrativo (racconto esperienza)
                 • VERSIONE 2: Valore (benefici concreti)
                 • VERSIONE 3: Conversazione (coinvolgimento)
               - Zero duplicati tra versioni
               - Quotes uniche e significative
            
            3. QUALITÀ CONTENUTISTICA:
               - Coerenza con brand identity
               - Adattamento alla piattaforma
               - Messaggio chiaro e impact
               - Call-to-action efficaci
            
            **FORMATO OUTPUT JSON:**
            {
              "socialPostVersions": [
                "Prima versione narrativa con lunghezza corretta...",
                "Seconda versione valore con approccio diverso...",
                "Terza versione conversazionale unica..."
              ],
              "headlineVersions": [
                "Headline prima versione",
                "Headline seconda versione", 
                "Headline terza versione"
              ],
              "shortQuoteVersions": [
                "Quote unica prima versione",
                "Quote diversa seconda versione",
                "Quote originale terza versione"
              ],
              "callToActionVersions": [
                "CTA prima versione",
                "CTA seconda versione",
                "CTA terza versione"
              ]
            }
            """.formatted(
                buildCompactBrandContext(brandProfile),
                inputText.length() > 400 ? inputText.substring(0, 400) + "..." : inputText,
                platform.toUpperCase(),
                postType != null ? postType : "Social Post",
                lengthConfig.getSocialPostLength(),
                lengthConfig.getHeadlineLength(),
                lengthConfig.getQuoteLength(),
                lengthConfig.getCtaLength(),
                emotion, getCompactEmotionDesc(emotion),
                creativity, getCompactCreativityDesc(creativity),
                formality, getCompactFormalityDesc(formality),
                urgency, getCompactUrgencyDesc(urgency)
            );
        
        return callGroqAPI(prompt, restTemplate, 
            "Generazione qualità (tentativo " + attempt + ")", 2000);
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

    // 📏 CALCOLO CONFIGURAZIONE LUNGHEZZA
    private LengthConfig calculateLengthConfig(int lengthPercentage) {
        if (lengthPercentage <= 5) return new LengthConfig(50, 25, 40, 20, "Micro");
        if (lengthPercentage <= 15) return new LengthConfig(100, 35, 50, 25, "Molto breve");
        if (lengthPercentage <= 25) return new LengthConfig(180, 45, 65, 30, "Breve");
        if (lengthPercentage <= 35) return new LengthConfig(250, 55, 80, 35, "Medio-breve");
        if (lengthPercentage <= 45) return new LengthConfig(350, 65, 95, 40, "Medio");
        if (lengthPercentage <= 55) return new LengthConfig(450, 75, 110, 45, "Medio-lungo");
        if (lengthPercentage <= 65) return new LengthConfig(550, 85, 125, 50, "Approfondito");
        if (lengthPercentage <= 75) return new LengthConfig(700, 95, 140, 55, "Esteso");
        if (lengthPercentage <= 85) return new LengthConfig(850, 105, 155, 60, "Molto esteso");
        if (lengthPercentage <= 95) return new LengthConfig(1000, 115, 170, 65, "Completo");
        return new LengthConfig(1200, 125, 185, 70, "Massimo");
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
            
            // 🎯 ESTRAZIONE CONTENUTI
            dto.setSocialPostVersions(extractList(contentNode, "socialPostVersions"));
            dto.setHeadlineVersions(extractList(contentNode, "headlineVersions"));
            dto.setShortQuoteVersions(extractList(contentNode, "shortQuoteVersions"));
            dto.setCallToActionVersions(extractList(contentNode, "callToActionVersions"));
            
            // 📊 LOG RISULTATI
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
        System.out.println("📐 Target di lunghezza:");
        System.out.println("   Social Posts: " + config.getSocialPostLength() + " caratteri");
        System.out.println("   Headlines: " + config.getHeadlineLength() + " caratteri");
        System.out.println("   Quotes: " + config.getQuoteLength() + " caratteri");
        System.out.println("   CTAs: " + config.getCtaLength() + " caratteri");
        
        System.out.println("\n📊 Contenuti generati:");
        
        // Social Posts
        List<String> socialPosts = dto.getSocialPostVersions();
        for (int i = 0; i < socialPosts.size(); i++) {
            String post = socialPosts.get(i);
            int length = post.length();
            double deviation = ((double)(length - config.getSocialPostLength()) / config.getSocialPostLength()) * 100;
            String status = Math.abs(deviation) <= 10 ? "✅" : "⚠️";
            System.out.println(String.format(
                "   %s Social Post %d: %d caratteri (%.1f%%)",
                status, i + 1, length, deviation
            ));
            
            if (i == 0) {
                System.out.println("   📝 Anteprima: \"" + 
                    (post.length() > 80 ? post.substring(0, 80) + "..." : post) + "\"");
            }
        }
        
        // Verifica diversità
        checkAndLogDiversity(dto);
        
        // Metriche aggiuntive se presenti
        if (root.has("optimizationScore")) {
            System.out.println("📈 Score ottimizzazione: " + root.get("optimizationScore").asInt() + "/100");
        }
    }

    // 🔍 VERIFICA E LOG DIVERSITÀ
    private void checkAndLogDiversity(TestimonialDTO dto) {
        Set<String> socialPostBeginnings = new HashSet<>();
        Set<String> quotes = new HashSet<>();
        
        for (String post : dto.getSocialPostVersions()) {
            if (post.length() > 0) {
                socialPostBeginnings.add(post.substring(0, Math.min(40, post.length())));
            }
        }
        
        for (String quote : dto.getShortQuoteVersions()) {
            if (quote.length() > 0) {
                quotes.add(quote);
            }
        }
        
        boolean socialPostsDiverse = socialPostBeginnings.size() >= 2;
        boolean quotesDiverse = quotes.size() >= 2;
        
        System.out.println("🎭 Diversità contenuti:");
        System.out.println("   Social Posts: " + (socialPostsDiverse ? "✅" : "⚠️") + 
                          " (" + socialPostBeginnings.size() + "/3 versioni distinte)");
        System.out.println("   Quotes: " + (quotesDiverse ? "✅" : "⚠️") + 
                          " (" + quotes.size() + "/3 versioni distinte)");
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
        System.out.println("📈 Richiesta accettata. Concorrenza: " + activeRequests.get() + "/" + MAX_CONCURRENT_REQUESTS + 
                          " | Token: " + projectedTokens + "/30000");
        return true;
    }

    private void recordTokenUsage(int tokens) {
        long currentUsage = tokensThisMinute.addAndGet(tokens);
        activeRequests.decrementAndGet();
        
        double usagePercentage = (currentUsage / 30000.0) * 100;
        System.out.println("📊 Token usati: " + currentUsage + "/30000 (" + String.format("%.1f", usagePercentage) + "%)");
        
        if (usagePercentage > 80) {
            System.out.println("⚠️  Attenzione: utilizzo token >80%");
        }
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
            System.out.println("✅ " + phase + ": Successo");
            
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
            Sei un copywriter esperto specializzato in contenuti social.
            
            COMPETENZE:
            • Copywriting strategico per social media
            • Adattamento multi-piattaforma (LinkedIn, Instagram, Facebook, Twitter)
            • Ottimizzazione engagement e conversione
            • Coerenza brand identity e tone of voice
            
            PRINCIPI:
            - Qualità e originalità sopra tutto
            - Coerenza assoluta con contesto e brand
            - Diversità di approcci tra le versioni
            - Rispetto preciso delle specifiche tecniche
            
            FORMATO: Restituisci SEMPRE JSON valido e ben formattato.
            """;
    }

    // 🏗️ CONTESTO BRAND
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

    // 🎪 METODI DI SUPPORTO
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
                // 🎯 RESTITUISCI SEMPRE 3 VERSIONI COME DA CONTRATTO
                while (result.size() < 3) {
                    result.add("Versione " + (result.size() + 1) + " - Contenuto generato");
                }
                return result;
            }
        } catch (Exception e) {
            System.err.println("⚠️ Errore estrazione " + key + ": " + e.getMessage());
        }
        
        // 🎯 SOLO IN CASO DI ERRORE GRAVE, CONTENUTI MINIMALI
        return Arrays.asList(
            "Esplora nuove possibilità con approccio innovativo",
            "Scopri come trasformare esperienza in risultati", 
            "Unisciti a percorso di eccellenza e innovazione"
        );
    }
}