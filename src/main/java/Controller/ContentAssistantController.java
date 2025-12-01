// Controller/ContentAssistantController.java
package Controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import DTO.Assistant.AssistantResponse;
import DTO.Assistant.ContentAnalysisRequest;
import Service.assistant.ContentAssistantService;

@RestController
@RequestMapping("/api/assistant")
public class ContentAssistantController {
    
    @Autowired
    private ContentAssistantService assistantService;
    
    @PostMapping("/analyze")
    public ResponseEntity<AssistantResponse> analyzeContent(
            @RequestBody ContentAnalysisRequest request) {
        try {
            System.out.println("🎯 Ricevuta richiesta analisi per brand: " + request.getBrandId());
            AssistantResponse response = assistantService.analyzeContent(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("❌ Errore controller assistant: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}