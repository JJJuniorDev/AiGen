package Service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import Enums.ToneType;
import model.BrandProfile;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class WebsiteAnalysisService {

    @Autowired
    private LLMService llmService;

    private static final Pattern URL_PATTERN = Pattern.compile(
        "^(https?://)?([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}(/.*)?$"
    );
    
    private static final Pattern COLOR_PATTERN = Pattern.compile(
        "(#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3}))|" +
        "(rgb\\(\\s*\\d+\\s*,\\s*\\d+\\s*,\\s*\\d+\\s*\\))"
    );

    public BrandProfile analyzeWebsiteAndCreateBrand(String websiteUrl, String language) {
        try {
            System.out.println("🔍 Analisi avanzata sito: " + websiteUrl);
            
            // 1. Estrazione contenuto strutturato
            Map<String, Object> websiteData = extractStructuredWebsiteData(websiteUrl);
            
            // 2. Analisi visiva
            Map<String, String> visualData = extractVisualElements((Document) websiteData.get("document"));
            
            // 3. Costruzione prompt dettagliato
            String prompt = buildEnhancedAnalysisPrompt(websiteData, visualData, websiteUrl, language);
            
            // 4. Chiamata LLM
            System.out.println("🧠 Analisi AI avanzata in corso...");
            String analysisResult = llmService.callGroqAPIForAnalysis(prompt, "Enhanced Brand Analysis", 3500);
            
            // 5. Parsing e creazione profilo
            return parseEnhancedBrandProfile(analysisResult, websiteData, visualData, websiteUrl);
            
        } catch (Exception e) {
            System.err.println("❌ Errore analisi avanzata: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private Map<String, Object> extractStructuredWebsiteData(String url) throws Exception {
        Map<String, Object> data = new HashMap<>();
        
        if (!url.startsWith("http")) {
            url = "https://" + url;
        }

        Document doc = Jsoup.connect(url)
            .userAgent("Mozilla/5.0 (compatible; BrandMind-Analyzer/2.0; +https://brandmind.ai)")
            .timeout(15000)
            .maxBodySize(1024 * 1024)
            .get();
        
        data.put("document", doc);
        data.put("url", url);
        
        // Estrazione gerarchica
        data.put("heroContent", extractSectionContent(doc, 
            new String[]{"section.hero", ".hero-section", ".jumbotron", "h1", ".headline"}, 3));
        
        data.put("valueProps", extractSectionContent(doc,
            new String[]{"[class*='value']", "[class*='benefit']", ".usp", ".advantage", "h2"}, 5));
        
        data.put("problemsSolved", extractProblemStatements(doc));
        
        // Meta informazioni
        data.put("meta", extractMetaInformation(doc));
        
        // Contenuto principale
        data.put("mainContent", extractMainContent(doc));
        
        // Analisi settoriale
        data.put("industryIndicators", analyzeIndustryIndicators(doc));
        
        System.out.println("✅ Dati strutturati estratti per: " + url);
        return data;
    }

    private List<String> extractSectionContent(Document doc, String[] selectors, int limit) {
        List<String> content = new ArrayList<>();
        
        for (String selector : selectors) {
            Elements elements = doc.select(selector);
            for (Element el : elements) {
                String text = el.text().trim();
                if (text.length() > 10 && text.length() < 300) {
                    content.add(text);
                }
            }
        }
        
        return content.stream()
            .distinct()
            .limit(limit)
            .collect(Collectors.toList());
    }

    private List<String> extractProblemStatements(Document doc) {
        List<String> problems = new ArrayList<>();
        
        // Cerca frasi che indicano problemi o sfide
        String[] problemKeywords = {"problema", "difficoltà", "sfida", "pain", 
                                   "fatica", "complesso", "ostacolo", "limite", 
                                   "difficile", "costoso", "lento"};
        
        Elements paragraphs = doc.select("p, li, h2, h3");
        for (Element el : paragraphs) {
            String text = el.text().toLowerCase();
            for (String keyword : problemKeywords) {
                if (text.contains(keyword) && text.length() > 20 && text.length() < 200) {
                    problems.add(el.text().trim());
                    break;
                }
            }
        }
        
        return problems.stream().distinct().limit(5).collect(Collectors.toList());
    }

    private Map<String, String> extractMetaInformation(Document doc) {
        Map<String, String> meta = new HashMap<>();
        
        meta.put("title", doc.title());
        meta.put("description", doc.select("meta[name=description]").attr("content"));
        meta.put("keywords", doc.select("meta[name=keywords]").attr("content"));
        meta.put("ogTitle", doc.select("meta[property=og:title]").attr("content"));
        meta.put("ogDescription", doc.select("meta[property=og:description]").attr("content"));
        
        // Author/company info
        meta.put("author", doc.select("meta[name=author]").attr("content"));
        meta.put("company", doc.select("meta[property='og:site_name']").attr("content"));
        
        return meta;
    }

    private String extractMainContent(Document doc) {
        StringBuilder content = new StringBuilder();
        
        // Estrai contenuto principale (evita header/footer)
        Elements mainElements = doc.select("main, article, .main-content, .content, #content");
        if (mainElements.isEmpty()) {
            mainElements = doc.select("body");
        }
        
        for (Element el : mainElements) {
            // Prendi solo h1-h3 e paragrafi significativi
            Elements textElements = el.select("h1, h2, h3, p");
            for (Element textEl : textElements) {
                String text = textEl.text().trim();
                if (text.length() > 20 && text.length() < 500) {
                    content.append(text).append("\n");
                }
            }
        }
        
        String fullContent = content.toString();
        return fullContent.length() > 3000 ? fullContent.substring(0, 3000) + "..." : fullContent;
    }

    private Map<String, String> extractVisualElements(Document doc) {
        Map<String, String> visualData = new HashMap<>();
        
        try {
            // Estrai colori dal CSS inline
            Set<String> colors = new HashSet<>();
            Elements styledElements = doc.select("[style]");
            for (Element el : styledElements) {
                String style = el.attr("style");
                extractColorsFromStyle(style, colors);
            }
            
            // Aggiungi colori da classi comuni
            if (colors.isEmpty()) {
                colors.addAll(Arrays.asList("#000000", "#FFFFFF", "#007BFF"));
            }
            
            // Analisi layout
            String layoutType = analyzeLayoutType(doc);
            
            visualData.put("colors", String.join(",", colors.stream().limit(5).collect(Collectors.toList())));
            visualData.put("layout", layoutType);
            visualData.put("hasImages", String.valueOf(doc.select("img[src]").size() > 3));
            visualData.put("hasVideo", String.valueOf(!doc.select("video, iframe[src*='youtube'], iframe[src*='vimeo']").isEmpty()));
            
        } catch (Exception e) {
            System.err.println("⚠️ Errore analisi visiva: " + e.getMessage());
            visualData.put("colors", "#000000,#FFFFFF,#007BFF");
            visualData.put("layout", "STANDARD");
        }
        
        return visualData;
    }

    private void extractColorsFromStyle(String style, Set<String> colors) {
        Matcher matcher = COLOR_PATTERN.matcher(style);
        while (matcher.find()) {
            String color = matcher.group();
            // Normalizza i colori RGB a HEX se necessario
            if (color.startsWith("rgb")) {
                color = rgbToHex(color);
            }
            if (color != null) {
                colors.add(color);
            }
        }
    }

    private String rgbToHex(String rgb) {
        try {
            rgb = rgb.replace("rgb(", "").replace(")", "");
            String[] parts = rgb.split(",");
            int r = Integer.parseInt(parts[0].trim());
            int g = Integer.parseInt(parts[1].trim());
            int b = Integer.parseInt(parts[2].trim());
            return String.format("#%02x%02x%02x", r, g, b);
        } catch (Exception e) {
            return null;
        }
    }

    private String analyzeLayoutType(Document doc) {
        // Analisi semplice del layout
        int gridItems = doc.select("[class*='grid'], [class*='col-']").size();
        int flexItems = doc.select("[class*='flex']").size();
        int containerItems = doc.select("[class*='container']").size();
        
        if (gridItems > 5) return "GRID_BASED";
        if (flexItems > 5) return "FLEXBOX";
        if (containerItems > 2) return "CONTAINER_CENTERED";
        
        return "CUSTOM_LAYOUT";
    }

    private Map<String, Integer> analyzeIndustryIndicators(Document doc) {
        Map<String, Integer> indicators = new HashMap<>();
        String fullText = doc.text().toLowerCase();
        
        // Mappa keyword -> settore
        Map<String, List<String>> industryKeywords = Map.of(
            "TECHNOLOGY", Arrays.asList("software", "app", "digitale", "tech", "sistema", "cloud", "api", "develop"),
            "FOOD_BEVERAGE", Arrays.asList("ristorante", "pizza", "menu", "cena", "chef", "cucina", "food", "drink"),
            "FASHION", Arrays.asList("abbigliamento", "moda", "collezione", "outfit", "stile", "design", "vestiti"),
            "HEALTH_FITNESS", Arrays.asList("salute", "benessere", "fitness", "allenamento", "nutrizione", "palestra"),
            "EDUCATION", Arrays.asList("corso", "formazione", "lezione", "apprendimento", "scuola", "università"),
            "FINANCE", Arrays.asList("finanza", "investimento", "prestito", "banca", "assicurazione", "mutuo"),
            "REAL_ESTATE", Arrays.asList("immobiliare", "casa", "appartamento", "affitto", "vendita", "agenzia")
        );
        
        for (Map.Entry<String, List<String>> entry : industryKeywords.entrySet()) {
            int score = 0;
            for (String keyword : entry.getValue()) {
                if (fullText.contains(keyword)) {
                    score++;
                }
            }
            if (score > 0) {
                indicators.put(entry.getKey(), score);
            }
        }
        
        return indicators;
    }

    private String buildEnhancedAnalysisPrompt(
        Map<String, Object> websiteData, 
        Map<String, String> visualData,
        String websiteUrl, 
        String language) {
        
        @SuppressWarnings("unchecked")
        List<String> heroContent = (List<String>) websiteData.get("heroContent");
        @SuppressWarnings("unchecked")
        List<String> valueProps = (List<String>) websiteData.get("valueProps");
        @SuppressWarnings("unchecked")
        List<String> problemsSolved = (List<String>) websiteData.get("problemsSolved");
        @SuppressWarnings("unchecked")
        Map<String, String> meta = (Map<String, String>) websiteData.get("meta");
        @SuppressWarnings("unchecked")
        Map<String, Integer> industryIndicators = (Map<String, Integer>) websiteData.get("industryIndicators");
        
        String mainContent = (String) websiteData.get("mainContent");
        
        // Costruisci contesto dettagliato
        StringBuilder context = new StringBuilder();
        context.append("=== ANALISI SITO WEB STRUTTURATA ===\n\n");
        
        context.append("1. HERO / MESSAGGIO PRINCIPALE:\n");
        heroContent.forEach(item -> context.append("   • ").append(item).append("\n"));
        
        context.append("\n2. PROPOSTE DI VALORE UNICHE:\n");
        valueProps.forEach(item -> context.append("   • ").append(item).append("\n"));
        
        context.append("\n3. PROBLEMI RISOLTI (dai clienti):\n");
        problemsSolved.forEach(item -> context.append("   • ").append(item).append("\n"));
        
        context.append("\n4. METADATI:\n");
        meta.forEach((key, value) -> {
            if (!value.isEmpty()) {
                context.append("   ").append(key).append(": ").append(value).append("\n");
            }
        });
        
        context.append("\n5. ANALISI VISIVA:\n");
        visualData.forEach((key, value) -> context.append("   ").append(key).append(": ").append(value).append("\n"));
        
        context.append("\n6. INDICATORI SETTORE:\n");
        industryIndicators.forEach((key, value) -> 
            context.append("   ").append(key).append(": ").append(value).append("\n"));
        
        context.append("\n7. CONTENUTO PRINCIPALE (estratto):\n");
        context.append(mainContent.substring(0, Math.min(1000, mainContent.length()))).append("\n");
        
        if ("it".equals(language)) {
            return """
                ANALISI BRAND AVANZATA - CREA PROFILO COMPLETO
                
                CONTESTO ESTRATTO DAL SITO:
                %s
                
                URL: %s
                
                ISTRUZIONI DETTAGLIATE:
                
                1. **IDENTITÀ FONDAMENTALE**:
                   - Nome brand (preciso, estrai dal contenuto)
                   - Posizionamento di mercato (max 1 frase: chi sono, per chi, perché unici)
                   - Mission (perché esistono)
                   - Vision (dove vogliono arrivare)
                   - Archetype brand (scegli il più pertinente): INNOVATORE, ESPERTO, AFFIDABILE, RIBELLE, PREMIUM, AMICHEVOLE
                
                2. **TONALITÀ DI VOCE** (analizza e seleziona):
                   - Tono principale (es: FORMALE_PROFESSIONALE, CASUALE_FRIENDLY, etc.)
                   - Descrizione tono (2-3 frasi che descrivono come comunicano)
                
                3. **VALORI E DIFFERENZIALI**:
                   - 3-5 valori brand principali
                   - 3-5 differenziali competitivi (cosa li distingue dai concorrenti)
                
                4. **GUIDA CONTENUTI**:
                   - 5-8 parole chiave preferite (basate su contenuto)
                   - 3-5 parole da evitare (incoerenti col brand)
                   - 3-5 parole da usare frequentemente
                
                5. **IDENTITÀ VISIVA**:
                   - Stile grafico (basato su analisi layout: MODERN_MINIMAL, BOLD_CREATIVE, CLASSIC_ELEGANT, PLAYFUL_FUN)
                   - Palette colori (3-5 colori principali in formato HEX)
                   - Categoria settore principale
                
                OUTPUT FORMATO JSON:
                {
                  "brandName": "Nome preciso del brand",
                  "positioning": "Posizionamento conciso",
                  "missionStatement": "Mission del brand",
                  "visionStatement": "Vision del brand",
                  "brandArchetype": "ARCHETYPE",
                  "tone": "FORMALE_PROFESSIONALE",
                  "voiceDescription": "Descrizione estesa del tono di voce",
                  "brandValues": "Valore1, Valore2, Valore3",
                  "competitiveDifferentials": ["Diff1", "Diff2", "Diff3"],
                  "preferredKeywords": ["keyword1", "keyword2"],
                  "avoidedWords": ["parola1", "parola2"],
                  "preferredWords": ["parola3", "parola4"],
                  "visualStyle": "MODERN_MINIMAL",
                  "colorPalette": ["#HEX1", "#HEX2", "#HEX3"],
                  "industryCategory": "SETTORE_PRINCIPALE"
                }
                
                REGOLE IMPORTANTI:
                - Basati SOLO sui dati forniti, non inventare
                - Sii specifico e concreto, evita genericismo
                - Per l'archetype, scegli quello che meglio si adatta ai contenuti
                - I colori suggeriti devono essere coerenti con l'analisi visiva fornita
                """.formatted(context.toString(), websiteUrl);
        } else {
            // Versione inglese simile
            return buildEnglishPrompt(context.toString(), websiteUrl);
        }
    }

    private BrandProfile parseEnhancedBrandProfile(
        String analysisResult, 
        Map<String, Object> websiteData,
        Map<String, String> visualData,
        String websiteUrl) {
        
        try {
            // Pulisci JSON
            String cleanJson = analysisResult
                .replaceAll("(?i)```json", "")
                .replaceAll("```", "")
                .trim();

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(cleanJson);
            
            BrandProfile profile = new BrandProfile();
            
            // Core Identity
            profile.setBrandName(root.path("brandName").asText("Brand Sconosciuto"));
            profile.setPositioning(root.path("positioning").asText(""));
            profile.setMissionStatement(root.path("missionStatement").asText(""));
            profile.setVisionStatement(root.path("visionStatement").asText(""));
            profile.setBrandArchetype(root.path("brandArchetype").asText(""));
            
            // Tone & Voice
            profile.setTone(ToneType.valueOf(root.path("tone").asText("FORMALE_PROFESSIONALE")));
            profile.setVoiceDescription(root.path("voiceDescription").asText(""));
            
            // Values & Competitives
            profile.setBrandValues(root.path("brandValues").asText(""));
            
            List<String> differentials = new ArrayList<>();
            root.path("competitiveDifferentials").forEach(diff -> differentials.add(diff.asText()));
            profile.setCompetitiveDifferentialsList(differentials);
            
            // Keywords & Words
            List<String> keywords = new ArrayList<>();
            root.path("preferredKeywords").forEach(kw -> keywords.add(kw.asText()));
            profile.setPreferredKeywords(keywords);
            
            List<String> avoidedWords = new ArrayList<>();
            root.path("avoidedWords").forEach(word -> avoidedWords.add(word.asText()));
            profile.setAvoidedWords(avoidedWords);
            
            List<String> preferredWords = new ArrayList<>();
            root.path("preferredWords").forEach(word -> preferredWords.add(word.asText()));
            profile.setPreferredWords(preferredWords);
       
            // Visual Identity
            profile.setVisualStyle(root.path("visualStyle").asText("MODERN_MINIMAL"));
            
            List<String> colors = new ArrayList<>();
            root.path("colorPalette").forEach(color -> colors.add(color.asText()));
            profile.setColorPaletteList(colors);
            
            profile.setIndustryCategory(root.path("industryCategory").asText(""));
            
            // Campi derivati dall'analisi (non dal JSON)
            @SuppressWarnings("unchecked")
            Map<String, Integer> industryIndicators = (Map<String, Integer>) websiteData.get("industryIndicators");
            String primaryIndustry = industryIndicators.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("GENERAL");
            
            if (profile.getIndustryCategory() == null || profile.getIndustryCategory().isEmpty()) {
                profile.setIndustryCategory(primaryIndustry);
            }
            
            // Target Audience dedotta
            String targetAudience = deduceTargetAudience(profile.getIndustryCategory(), 
                                                         profile.getBrandArchetype());
            profile.setTargetAudience(targetAudience);
            
            // Hashtag di default basati su industria
            List<String> hashtags = generateDefaultHashtags(profile.getIndustryCategory(), 
                                                           profile.getBrandName());
            profile.setDefaultHashtags(hashtags);
            
            System.out.println("✅ Profilo avanzato creato: " + profile.getBrandName());
            return profile;
            
        } catch (Exception e) {
            System.err.println("❌ Errore parsing analisi avanzata: " + e.getMessage());
            System.err.println("Raw response: " + analysisResult.substring(0, Math.min(200, analysisResult.length())));
        return null;
        }
    }

    private String deduceTargetAudience(String industry, String archetype) {
        Map<String, String> targetMap = new HashMap<>();
        targetMap.put("TECHNOLOGY", "Professionisti digitali 25-45 anni, imprenditori tech, early adopters");
        targetMap.put("FOOD_BEVERAGE", "Foodies 20-50 anni, famiglie, turisti, amanti della buona cucina");
        targetMap.put("FASHION", "Persone fashion-conscious 18-40 anni, trendsetters, shopping enthusiasts");
        targetMap.put("HEALTH_FITNESS", "Persone attive 20-60 anni, fitness enthusiasts, wellness seekers");
        targetMap.put("EDUCATION", "Studenti, professionisti in formazione, lifelong learners");
        targetMap.put("FINANCE", "Professionisti 30-60 anni, investitori, famiglie, imprenditori");
        
        return targetMap.getOrDefault(industry, 
            "Clienti interessati a prodotti/servizi di qualità, professionisti");
    }

    private List<String> generateDefaultHashtags(String industry, String brandName) {
        List<String> hashtags = new ArrayList<>();
        
        // Hashtag generici
        hashtags.add("#" + brandName.replaceAll("\\s+", "").toLowerCase());
        
        // Hashtag specifici per settore
        Map<String, List<String>> industryHashtags = Map.of(
            "TECHNOLOGY", Arrays.asList("#tech", "#innovazione", "#digital", "#startup"),
            "FOOD_BEVERAGE", Arrays.asList("#food", "#cucina", "#ristorante", "#buonocibo"),
            "FASHION", Arrays.asList("#moda", "#style", "#fashion", "#outfit"),
            "HEALTH_FITNESS", Arrays.asList("#fitness", "#salute", "#benessere", "#workout"),
            "EDUCATION", Arrays.asList("#formazione", "#apprendimento", "#corsi", "#education"),
            "FINANCE", Arrays.asList("#finanza", "#investimenti", "#economia", "#business")
        );
        
        hashtags.addAll(industryHashtags.getOrDefault(industry, 
            Arrays.asList("#business", "#quality", "#professional")));
        
        return hashtags.stream().limit(5).collect(Collectors.toList());
    }


    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    private String buildEnglishPrompt(String context, String websiteUrl) {
        // Implementazione simile alla versione italiana ma in inglese
        return """
            ADVANCED BRAND ANALYSIS - CREATE COMPLETE PROFILE
            
            EXTRACTED WEBSITE CONTEXT:
            %s
            
            URL: %s
            
            ... (similar instructions in English) ...
            """.formatted(context, websiteUrl);
    }
}