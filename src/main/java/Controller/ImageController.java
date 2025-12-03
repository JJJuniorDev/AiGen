package Controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import DTO.SaveImageRequest;
import DTO.SocialImageBatchRequest;
import DTO.SocialImageRequest;
import DTO.SocialImageResponse;
import Service.ImageGenerationService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/images")
public class ImageController {
 
    private static final Logger log = LoggerFactory.getLogger(ImageController.class);
    
    @Autowired
    private ImageGenerationService imageGenerationService;
    
    // Costruttore con dependency injection
    public ImageController() {
    }
    
    @PostMapping("/generate")
    public ResponseEntity<?> generateSingleImage(@RequestBody SocialImageRequest request) {
        try {
            SocialImageResponse response = imageGenerationService.generateSocialImage(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
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
}