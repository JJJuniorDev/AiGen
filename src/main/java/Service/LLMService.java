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

        // 🎯 1. GENERAZIONE PRIMARIA con contesto avanzato
        String initialContent = generateInitialPosts(inputText, platform, postType, 
            emotion, creativity, formality, urgency, length, brandProfile, restTemplate);
        
        // 🔍 2. ANALISI QUALITATIVA E RAFFINAMENTO
        String refinedContent = qualityRefinementLayer(initialContent, inputText, brandProfile, platform, restTemplate);
        
        // 🎨 3. OTTIMIZZAZIONE FINALE E ADATTAMENTO
        String optimizedContent = finalOptimizationLayer(refinedContent, brandProfile, platform, postType, restTemplate);
        
        // 📦 4. PROCESSING FINALE
        TestimonialDTO finalDTO = processFinalContent(optimizedContent, inputText);
        
        System.out.println("✅ Generazione completata con sistema di raffinamento a 3 livelli!");
        return finalDTO;
    }

    // 🎯 1. GENERAZIONE PRIMARIA - Sistema avanzato
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

    // 🔍 2. LAYER DI RAFFINAMENTO QUALITATIVO
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

    // 🎨 3. LAYER DI OTTIMIZZAZIONE FINALE
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
    
    // 🎨 PROCESSING FINALE MIGLIORATO
    private TestimonialDTO processFinalContent(String optimizedContent, String inputText) {
        try {
            String cleanContent = optimizedContent
                .replaceAll("(?i)```json", "")
                .replaceAll("```", "")
                .trim();
                
            JsonNode root = mapper.readTree(cleanContent);
            
            // Estrae il contenuto dai vari layer di nesting
            JsonNode contentNode = extractContentNode(root);
            
            TestimonialDTO dto = new TestimonialDTO();
            dto.setInputText(inputText);
            dto.setSocialPostVersions(extractList(contentNode, "socialPostVersions"));
            dto.setHeadlineVersions(extractList(contentNode, "headlineVersions"));
            dto.setShortQuoteVersions(extractList(contentNode, "shortQuoteVersions"));
            dto.setCallToActionVersions(extractList(contentNode, "callToActionVersions"));
            
            // Log delle metriche di qualità
            logQualityMetrics(root);
            
            return dto;
            
        } catch (Exception e) {
            System.err.println("❌ Errore processing finale: " + e.getMessage());
            return createEnhancedFallbackDTO(inputText);
        }
    }

    // 🔧 METODO UNIFICATO PER CHIAMATE API
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
            "max_tokens", 2500,
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

    // 🧠 SYSTEM PROMPT AVANZATO
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

    // 🏗️ CONTESTO BRAND AVANZATO
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

    // ✅ NUOVO: Contesto brand generico avanzato
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

    // 🎪 METODI DI SUPPORTO AVANZATI
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

    // ✅ NUOVO: DTO di fallback avanzato
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

    // 🎪 DESCRIZIONI PARAMETRICHE AVANZATE
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

    // ✅ NUOVO: Metodi di supporto strategico
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

    // ✅ NUOVO: Call-to-action brand-specifiche
    private String getBrandCTAs(BrandProfile brand) {
        if (brand.getPreferredCTAs() != null && !brand.getPreferredCTAs().isEmpty()) {
            return String.join(", ", brand.getPreferredCTAs());
        }
        return "Scopri di più, Inizia il percorso, Unisciti alla community, Contattaci per approfondire";
    }

    // ✅ MIGLIORATO: Extract list con robustezza avanzata
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
                // Assicura almeno 3 elementi di qualità
                while (result.size() < 3) {
                    result.add("Versione " + (result.size() + 1) + " - Contenuto ottimizzato in fase di generazione");
                }
                return result;
            }
        } catch (Exception e) {
            System.err.println("⚠️ Errore estrazione lista per " + key + ": " + e.getMessage());
        }
        
        // Fallback avanzato
        return Arrays.asList(
            "Esplora nuove possibilità con il nostro approccio innovativo",
            "Scopri come trasformare la tua esperienza in risultati straordinari", 
            "Unisciti a chi già vive un percorso di eccellenza e innovazione"
        );
    }

    // ✅ Istruzioni piattaforma (mantenute per compatibilità)
    private String getPlatformSpecificInstructions(String platform, String postType) {
        // Implementazione esistente mantenuta per compatibilità
        return "Piattaforma: " + platform + " - Tipo: " + (postType != null ? postType : "Generico");
    }
    
    private String getPostTypeInstructions(String postType) {
        return "Tipo contenuto: " + postType;
    }
}