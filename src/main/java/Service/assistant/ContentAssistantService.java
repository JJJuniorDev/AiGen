package Service.assistant;

import DTO.*;
import DTO.Assistant.AssistantResponse;
import DTO.Assistant.ContentAnalysisRequest;
import DTO.Assistant.ContentSuggestion;
import Enums.AnalysisType;
import model.BrandProfile;
import model.Content;
import Repository.BrandProfileRepository;
import Repository.UserContentHistoryRepository;
import Service.LLMService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ContentAssistantService {

 @Autowired
 private LLMService llmService;
 
 @Autowired
 private BrandProfileRepository brandRepository;
 
 @Autowired
 private UserContentHistoryRepository contentHistoryRepo;
 
 private final ObjectMapper mapper = new ObjectMapper();
 
 public AssistantResponse analyzeContent(ContentAnalysisRequest request) {
     try {
         System.out.println("🔍 Content Assistant - Analisi in corso...");
         Long brandId= (Long.valueOf(request.getBrandId()));
         // 1. Recupera contesto brand
         BrandProfile brand = brandRepository.findById(brandId)
             .orElseThrow(() -> new RuntimeException("Brand non trovato"));
         
         // 2. Recupera storico contenuti per apprendimento
         List<Content> recentContent = contentHistoryRepo.findTop5ByBrandIdOrderByCreatedAtDesc(brandId);
         
         // 3. Costruisce prompt contestualizzato
         String analysisPrompt = buildAnalysisPrompt(
             request.getContent(), 
             brand, 
             recentContent, 
             request.getAnalysisType(),
             request.getPlatform()
         );
         
         // 4. Chiama AI per analisi
         String aiResponse = llmService.callContentAssistantAPI(analysisPrompt);
         
         // 5. Parsing e ritorno risposta strutturata
         return parseAssistantResponse(aiResponse);
         
     } catch (Exception e) {
         System.err.println("❌ Errore Content Assistant: " + e.getMessage());
        e.printStackTrace();
        return null;
     }
 }
 
 private String buildAnalysisPrompt(String content, BrandProfile brand, 
                                  List<Content> history, AnalysisType type,
                                  String platform) {
     
     String historyContext = formatContentHistory(history);
     
     String brandName = brand.getBrandName();
     String tone = brand.getTone().toString();
     String targetAudience = brand.getTargetAudience() != null ? brand.getTargetAudience() : "Non specificato";
     String brandValues = brand.getBrandValues() != null ? brand.getBrandValues() : "Professionalità, Qualità";
     String avoidedWords = brand.getAvoidedWords() != null ? String.join(", ", brand.getAvoidedWords()) : "Nessuna";
     String analysisTypeName = type.getDisplayName();
     
     StringBuilder prompt = new StringBuilder();
     prompt.append("Sei un esperto assistant di content marketing specializzato per il brand: ").append(brandName).append("\n\n");
     prompt.append("CONTESTO BRAND:\n");
     prompt.append("- Nome: ").append(brandName).append("\n");
     prompt.append("- Tono: ").append(tone).append("\n");
     prompt.append("- Target Audience: ").append(targetAudience).append("\n");
     prompt.append("- Valori: ").append(brandValues).append("\n");
     prompt.append("- Keywords preferite: ").append(avoidedWords).append("\n");
     prompt.append("- Parole da evitare: ").append(avoidedWords).append("\n\n");
     
     prompt.append("STORICO CONTENUTI RECENTI DEL BRAND (per mantenere coerenza):\n");
     prompt.append(historyContext).append("\n\n");
     
     prompt.append("CONTENUTO DA ANALIZZARE:\n");
     prompt.append("\"").append(content).append("\"\n\n");
     
     prompt.append("PIATTAFORMA DESTINAZIONE: ").append(platform).append("\n");
     prompt.append("TIPO DI ANALISI RICHIESTO: ").append(analysisTypeName).append("\n\n");
     
     prompt.append("""
         ANALIZZA E FORNISCI:
         1. PUNTI DI FORZA (2-3 cose che funzionano bene nel contenuto)
         2. AREE DI MIGLIORAMENTO (2-3 cose da ottimizzare)
         3. SUGGERIMENTI CONCRETI (riscritture specifiche con motivazioni)
         4. CONSIGLI SPECIFICI per la piattaforma %s
         5. SCORE QUALITÀ complessivo (1-10)
         6. CONFIDENCE LEVEL (0.0-1.0)

         SUGGERIMENTI OBBLIGATORI:
         - Per TONO: mostrare versione migliorata mantenendo messaggio core
         - Per ENGAGEMENT: suggerire hook, CTA, domande
         - Per BRAND: allineare a valori e keywords del brand
         - Per PIATTAFORMA: ottimizzare per formati e best practices

         FORMATO RISPOSTA JSON STRETTO:
         {
           "strengths": ["punto1", "punto2"],
           "improvements": ["miglioramento1", "miglioramento2"],
           "suggestions": [
             {
               "type": "TONO|ENGAGEMENT|BRAND|PLATFORM",
               "current": "testo originale",
               "suggested": "testo migliorato",
               "reason": "motivazione chiara"
             }
           ],
           "platformTips": ["tip1", "tip2"],
           "qualityScore": 7,
           "confidence": 0.85
         }

         Regole:
         - Sii costruttivo e specifico
         - Basati SOLO sul contesto fornito
         - Massimo 3 suggerimenti
         - Score basato su rilevanza brand + engagement potenziale
         """.formatted(platform));
     
     return prompt.toString();
 }
 
 private String formatContentHistory(List<Content> history) {
     if (history == null || history.isEmpty()) {
         return "Nessun contenuto storico disponibile";
     }
     
     StringBuilder sb = new StringBuilder();
     for (int i = 0; i < Math.min(history.size(), 3); i++) {
         Content content = history.get(i);
         sb.append(i + 1).append(". ").append(content.getContent()).append("\n");
     }
     return sb.toString();
 }
 
 private AssistantResponse parseAssistantResponse(String aiResponse) {
     try {
         String cleanJson = aiResponse
             .replaceAll("(?i)```json", "")
             .replaceAll("```", "")
             .trim();
             
         JsonNode root = mapper.readTree(cleanJson);
         
         // Parse strengths
         List<String> strengths = new ArrayList<>();
         if (root.has("strengths") && root.get("strengths").isArray()) {
             root.get("strengths").forEach(node -> strengths.add(node.asText()));
         }
         
         // Parse improvements
         List<String> improvements = new ArrayList<>();
         if (root.has("improvements") && root.get("improvements").isArray()) {
             root.get("improvements").forEach(node -> improvements.add(node.asText()));
         }
         
         // Parse suggestions
         List<ContentSuggestion> suggestions = new ArrayList<>();
         if (root.has("suggestions") && root.get("suggestions").isArray()) {
             for (JsonNode suggestionNode : root.get("suggestions")) {
                 ContentSuggestion suggestion = new ContentSuggestion(
                     suggestionNode.has("type") ? suggestionNode.get("type").asText() : "GENERAL",
                     suggestionNode.has("current") ? suggestionNode.get("current").asText() : "",
                     suggestionNode.has("suggested") ? suggestionNode.get("suggested").asText() : "",
                     suggestionNode.has("reason") ? suggestionNode.get("reason").asText() : ""
                 );
                 suggestions.add(suggestion);
             }
         }
         
         // Parse platform tips
         List<String> platformTips = new ArrayList<>();
         if (root.has("platformTips") && root.get("platformTips").isArray()) {
             root.get("platformTips").forEach(node -> platformTips.add(node.asText()));
         }
         
         // Parse scores
         int qualityScore = root.has("qualityScore") ? root.get("qualityScore").asInt() : 5;
         double confidence = root.has("confidence") ? root.get("confidence").asDouble() : 0.7;
         
         return new AssistantResponse(strengths, improvements, suggestions, platformTips, qualityScore, confidence);
         
     } catch (Exception e) {
         System.err.println("❌ Errore parsing risposta AI: " + e.getMessage());
          e.printStackTrace();
          return null;
     }
 }
 
 
}