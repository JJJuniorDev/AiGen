package Controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import DTO.SaveImageRequest;
import DTO.SocialImageBatchRequest;
import DTO.SocialImageRequest;
import DTO.SocialImageResponse;
import DTO.TestimonialDTO;
import DTO.UserDTO;
import Service.ImageGenerationService;
import Service.UserService;
import model.User;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/images")
public class ImageController {
 
    private static final Logger log = LoggerFactory.getLogger(ImageController.class);
    
    @Autowired
    private ImageGenerationService imageGenerationService;
    
    @Autowired 
    private UserService userService;
    
    // Costruttore con dependency injection
    public ImageController() {
    }
    
    @PostMapping("/generate")
    public ResponseEntity<?> generateSingleImage(@RequestBody SocialImageRequest request) {
    	
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) return ResponseEntity.status(401).build();

        UserDTO principal = (UserDTO) auth.getPrincipal();
        Optional<User> uOpt = userService.findById(Long.parseLong(principal.getId()));
        if (uOpt.isEmpty()) return ResponseEntity.status(401).build();

        User user = uOpt.get();
        // ✅ VERIFICA CREDITI AGGIORNATA
        if (!userService.useCredit(user, "CREAZIONE IMMAGINE")) {
            return ResponseEntity.status(402).body(createNoCreditsResponse());
        }
    	log.info("📨 Ricevuta richiesta - Prompt: {}", request.getPrompt());
    	 boolean isEditMode = request.getBaseImage() != null 
                 && !request.getBaseImage().isEmpty();
        try {
        	 SocialImageResponse response;
        	 log.info("✏️ Modifica immagine esistente per {}", request.getBrandName());
        	    log.info("📝 Nuovo prompt: {}", request.getPrompt());
        	    log.info("🎨 Stile richiesto: {}", request.getStyle()); // Aggiungi questo log
        	    log.info("🖼️ Base64 presente: {}", request.getBaseImage() != null && !request.getBaseImage().isEmpty());
        	    log.info("🔢 Numero edit: {}", request.getEditCount());
        	 if (isEditMode) {
                 log.info("🔄 Modalità EDIT - Immagine base presente");
                 response = imageGenerationService.editSocialImage(request);
             } else {
                 log.info("🆕 Modalità NEW - Generazione da testo");
                 response = imageGenerationService.generateSocialImage(request);
             }
        	 return ResponseEntity.ok(response);
        } catch (Exception e) {
        	 log.error("❌ Errore nel controller: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * 🔥 ENDPOINT: Genera BATCH di immagini (per più post)
     */
    @PostMapping("/generate/batch")
    public ResponseEntity<?> generateBatchImages(@RequestBody SocialImageBatchRequest request) {
        try {
            List<SocialImageResponse> images = imageGenerationService.generateBatchImages(request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("images", images);
            response.put("count", images.size());
            response.put("totalCost", images.size() * 0.5); // 0.5 crediti per immagine
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    @PostMapping("/save")
    public ResponseEntity<?> saveImage(@RequestBody SaveImageRequest request) {
        try {
            SocialImageResponse savedImage = imageGenerationService.saveImageToCloudinary(
                request.getImageBase64(),
                request.getPlatform(),
                request.getBrandName()
            );
            
            return ResponseEntity.ok(savedImage);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Errore nel salvataggio: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    
    /**
     * Controlla crediti necessari
     */
    @PostMapping("/estimate-cost")
    public ResponseEntity<?> estimateCost(@RequestBody Map<String, Object> request) {
        try {
            int numImages = (int) request.getOrDefault("numImages", 1);
            double costPerImage = 0.5; // Crediti per immagine
            
            Map<String, Object> response = new HashMap<>();
            response.put("requiredCredits", numImages * costPerImage);
            response.put("costPerImage", costPerImage);
            response.put("canGenerate", true);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    private TestimonialDTO createNoCreditsResponse() {
        TestimonialDTO dto = new TestimonialDTO();
        dto.setSocialPostVersions(Arrays.asList("Crediti insufficienti. Acquista altri crediti per continuare a generare contenuti."));
        dto.setHeadlineVersions(Arrays.asList("Crediti Esauriti"));
        dto.setShortQuoteVersions(Arrays.asList("Aggiorna il tuo piano"));
        dto.setCallToActionVersions(Arrays.asList("Visita la pagina dei piani per acquistare crediti"));
        return dto;
    }
}