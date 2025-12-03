package Service;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import DTO.SocialImageBatchRequest;
import DTO.SocialImageRequest;
import DTO.SocialImageResponse;

@Service
public class ImageGenerationService {

    private static final Logger log = LoggerFactory.getLogger(ImageGenerationService.class);
    
    @Value("${stability.api.key:}")
    private String stabilityApiKey;
    
    @Value("${cloudinary.cloud.name:}")
    private String cloudinaryCloudName;
    
    @Value("${cloudinary.upload.preset:}")
    private String cloudinaryUploadPreset;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    public ImageGenerationService() {
        // Costruttore vuoto - non serve LLMService
    }
    
    public List<SocialImageResponse> generateBatchImages(SocialImageBatchRequest request) {
        log.info("🎨 Generazione batch di {} immagini per {}", 
                 request.getPosts().size(), request.getBrandName());
        
        List<SocialImageResponse> images = new ArrayList<>();
        
        // Genera immagine per ogni post
        for (int i = 0; i < request.getPosts().size(); i++) {
            try {
                String postContent = request.getPosts().get(i);
                
                // Crea richiesta singola
                SocialImageRequest singleRequest = new SocialImageRequest();
                singleRequest.setContent(postContent);
                singleRequest.setPlatform(request.getPlatform());
                singleRequest.setBrandName(request.getBrandName());
                singleRequest.setStyle(request.getStyle());
                singleRequest.setIncludeText(request.isIncludeText());
                
                // Genera immagine (SOLO Base64, NO upload automatico)
                SocialImageResponse response = generateSocialImage(singleRequest);
                images.add(response);
                
                log.info("✅ Immagine {} generata (Base64)", i + 1);
                
                // Piccola pausa per non sovraccaricare le API
                if (i < request.getPosts().size() - 1) {
                    Thread.sleep(1000);
                }
                
            } catch (Exception e) {
                log.error("❌ Errore generazione immagine {}: {}", i + 1, e.getMessage());
                // Continua con le altre immagini
            }
        }
        
        return images;
    }
    
    /**
     * Genera singola immagine social (restituisce Base64 direttamente)
     */
    public SocialImageResponse generateSocialImage(SocialImageRequest request) {
        log.info("🎨 Generazione immagine per {}", request.getBrandName());
        
        try {
            // 1. Crea prompt ottimizzato
            String imagePrompt = createEnhancedPrompt(request);
            log.info("📝 Prompt: {}", imagePrompt);
            
            // 2. Genera immagine AI e ottieni Base64
            String imageBase64 = generateAIImageBase64(imagePrompt, request);
            
            // 3. Crea risposta con Base64 (NO upload automatico a Cloudinary)
            SocialImageResponse response = new SocialImageResponse();
            response.setImageBase64(imageBase64); // Imposta Base64
            response.setImageUrl(null); // URL Cloudinary vuoto
            response.setPromptUsed(imagePrompt);
            response.setPlatform(request.getPlatform());
            response.setDimensions(getDimensionsForPlatform(request.getPlatform()));
            response.setGeneratedAt(new Date());
            response.setSavedToCloudinary(false); // Flag: non salvato su Cloudinary
            response.setTemporaryId(UUID.randomUUID().toString()); // ID temporaneo
            
            log.info("✅ Immagine generata (Base64, {} chars)", imageBase64.length());
            
            return response;
            
        } catch (Exception e) {
            log.error("❌ Errore generazione immagine: {}", e.getMessage());
            throw new RuntimeException("Impossibile generare l'immagine: " + e.getMessage());
        }
    }
    
    /**
     * 🔥 NUOVO METODO: Genera immagine e restituisce Base64 (senza upload)
     */
    private String generateAIImageBase64(String prompt, SocialImageRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + stabilityApiKey);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            
            // Corpo richiesta
            Map<String, Object> requestBody = new HashMap<>();
            
            // Prompts
            List<Map<String, Object>> textPrompts = new ArrayList<>();
            
            Map<String, Object> positivePrompt = new HashMap<>();
            positivePrompt.put("text", prompt);
            positivePrompt.put("weight", 1.0);
            textPrompts.add(positivePrompt);
            
            Map<String, Object> negativePrompt = new HashMap<>();
            negativePrompt.put("text", "ugly, blurry, low quality, text, watermark, signature, letters, words");
            negativePrompt.put("weight", -1.0);
            textPrompts.add(negativePrompt);
            
            requestBody.put("text_prompts", textPrompts);
            
            // Dimensioni
            Map<String, Integer> dimensions = getDimensionsForPlatform(request.getPlatform());
            requestBody.put("width", dimensions.get("width"));
            requestBody.put("height", dimensions.get("height"));
            
            // Parametri qualità
            requestBody.put("cfg_scale", 7);
            requestBody.put("samples", 1);
            requestBody.put("steps", 30);
            requestBody.put("style_preset", "photographic");
            
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
            
            log.info("🔄 Invio a Stability AI... Dimensioni: {}x{}", 
                     dimensions.get("width"), dimensions.get("height"));
            
            // Chiama API
            ResponseEntity<Map> response = restTemplate.exchange(
                "https://api.stability.ai/v1/generation/stable-diffusion-xl-1024-v1-0/text-to-image",
                HttpMethod.POST,
                requestEntity,
                Map.class
            );
            
            // Estrai Base64 dalla risposta
            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("artifacts")) {
                List<Map<String, Object>> artifacts = (List<Map<String, Object>>) responseBody.get("artifacts");
                if (!artifacts.isEmpty() && artifacts.get(0).containsKey("base64")) {
                    String base64Image = (String) artifacts.get(0).get("base64");
                    log.info("✅ Base64 ricevuto ({} chars)", base64Image.length());
                    return base64Image;
                }
            }
            
            throw new RuntimeException("Nessuna immagine Base64 generata dall'API");
            
        } catch (Exception e) {
            log.error("❌ Errore Stability AI: {}", e.getMessage());
            throw new RuntimeException("Errore generazione immagine AI");
        }
    }
    
    /**
     * 🔥 NUOVO METODO: Salva un'immagine Base64 su Cloudinary (su richiesta)
     */
    public SocialImageResponse saveImageToCloudinary(String imageBase64, String platform, String brandName) {
        try {
            // Decodifica base64
            byte[] imageBytes = Base64.getDecoder().decode(imageBase64);
            
            // Headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            
            ByteArrayResource resource = new ByteArrayResource(imageBytes) {
                @Override
                public String getFilename() {
                    return "social-post-" + platform + "-" + System.currentTimeMillis() + ".png";
                }
            };
            
            body.add("file", resource);
            body.add("upload_preset", cloudinaryUploadPreset);
            body.add("folder", "social_posts");
            body.add("tags", "ai_generated,social_media," + platform + "," + brandName);
            
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            
            String uploadUrl = "https://api.cloudinary.com/v1_1/" + cloudinaryCloudName + "/image/upload";
            
            log.info("☁️ Upload a Cloudinary per {}...", brandName);
            
            // Invia a Cloudinary
            ResponseEntity<Map> response = restTemplate.exchange(
                uploadUrl,
                HttpMethod.POST,
                requestEntity,
                Map.class
            );
            
            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("secure_url")) {
                String imageUrl = (String) responseBody.get("secure_url");
                log.info("✅ Upload completato: {}", imageUrl);
                
                // Crea risposta con URL Cloudinary
                SocialImageResponse savedResponse = new SocialImageResponse();
                savedResponse.setImageUrl(imageUrl);
                savedResponse.setPlatform(platform);
                savedResponse.setDimensions(getDimensionsForPlatform(platform));
                savedResponse.setGeneratedAt(new Date());
                savedResponse.setSavedToCloudinary(true);
                
                return savedResponse;
            }
            
            throw new RuntimeException("Nessun URL dall'upload Cloudinary");
            
        } catch (Exception e) {
            log.error("❌ Errore Cloudinary: {}", e.getMessage());
            throw new RuntimeException("Errore upload immagine su Cloudinary");
        }
    }


    private String createEnhancedPrompt(SocialImageRequest request) {
        String platform = request.getPlatform().toLowerCase();
        String brand = request.getBrandName();
        String content = request.getContent();
        
        // Mappa stili
        Map<String, String> styleKeywords = new HashMap<>();
        styleKeywords.put("realistic", "photorealistic, high detail");
        styleKeywords.put("illustrative", "vector illustration, flat design");
        styleKeywords.put("minimal", "minimalist, clean, simple");
        styleKeywords.put("vibrant", "vibrant colors, bold, eye-catching");
        
        String requestStyle = request.getStyle(); // Usa un nome diverso
        String styleKey = (requestStyle != null && !requestStyle.isEmpty()) ? requestStyle : "realistic";
        String styleDesc = styleKeywords.getOrDefault(styleKey, "professional");
        
        // Mappa piattaforme
        Map<String, String> platformContext = new HashMap<>();
        platformContext.put("linkedin", "professional business, corporate, networking");
        platformContext.put("instagram", "aesthetic, trendy, social media, visually appealing");
        platformContext.put("twitter", "bold, attention-grabbing, conversational");
        platformContext.put("facebook", "community, friendly, engaging");
        
        String platformContextStr = platformContext.getOrDefault(platform, "social media");
        
        // Prompt finale
        return String.format(
            "%s social media post for %s brand. Content theme: %s. %s style. " +
            "High quality, professional design, no text overlay, perfect composition. " +
            "Trending on ArtStation, Behance, Dribbble.",
            platformContextStr, brand, 
            content.length() > 100 ? content.substring(0, 100) : content,
            styleDesc
        );
    }
    
    /**
     * Dimensioni per piattaforma
     */
    private Map<String, Integer> getDimensionsForPlatform(String platform) {
        Map<String, Integer> dimensions = new HashMap<>();
        
        switch (platform.toLowerCase()) {
        case "linkedin":
            // LinkedIn: preferisco 1152x896 (1.29:1) o 1216x832 (1.46:1)
            // L'originale 1200x627 = 1.91:1 → scegliamo 1216x832 = 1.46:1 (vicino)
            dimensions.put("width", 1216);
            dimensions.put("height", 832);
            break;
        case "twitter":
            // Twitter: 1200x675 = 1.78:1 → 1344x768 = 1.75:1 (vicino)
            dimensions.put("width", 1344);
            dimensions.put("height", 768);
            break;
        case "facebook":
            // Facebook: 1200x630 = 1.90:1 → 1216x832 = 1.46:1 
            // Oppure 1152x896 = 1.29:1
            dimensions.put("width", 1216);
            dimensions.put("height", 832);
            break;
        case "instagram":
        default:
            // Instagram: quadrato 1080x1080 → 1024x1024
            dimensions.put("width", 1024);
            dimensions.put("height", 1024);
            break;
    }
    
    log.debug("Dimensioni SDXL per {}: {}x{}", platform, 
             dimensions.get("width"), dimensions.get("height"));
    
    return dimensions;
    }
    
    /**
     * Genera immagine con Stability AI
     */
    private String generateAIImage(String prompt, SocialImageRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + stabilityApiKey);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            // Corpo richiesta
            Map<String, Object> requestBody = new HashMap<>();
            
            // Prompts
            List<Map<String, Object>> textPrompts = new ArrayList<>();
            
            Map<String, Object> positivePrompt = new HashMap<>();
            positivePrompt.put("text", prompt);
            positivePrompt.put("weight", 1.0);
            textPrompts.add(positivePrompt);
            
            Map<String, Object> negativePrompt = new HashMap<>();
            negativePrompt.put("text", "ugly, blurry, low quality, text, watermark, signature, letters, words");
            negativePrompt.put("weight", -1.0);
            textPrompts.add(negativePrompt);
            
            requestBody.put("text_prompts", textPrompts);
            
            // Dimensioni
            Map<String, Integer> dimensions = getDimensionsForPlatform(request.getPlatform());
            requestBody.put("width", dimensions.get("width"));
            requestBody.put("height", dimensions.get("height"));
            
            // Parametri qualità
            requestBody.put("cfg_scale", 7);
            requestBody.put("samples", 1);
            requestBody.put("steps", 30);
            requestBody.put("style_preset", "photographic");
            
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
            
            log.info("🔄 Invio a Stability AI... Dimensioni: {}x{}", 
                     dimensions.get("width"), dimensions.get("height"));
            
            // Chiama API
            ResponseEntity<Map> response = restTemplate.exchange(
                "https://api.stability.ai/v1/generation/stable-diffusion-xl-1024-v1-0/text-to-image",
                HttpMethod.POST,
                requestEntity,
                Map.class
            );
            
            // Estrai immagine base64
            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("artifacts")) {
                List<Map<String, Object>> artifacts = (List<Map<String, Object>>) responseBody.get("artifacts");
                if (!artifacts.isEmpty()) {
                    String base64Image = (String) artifacts.get(0).get("base64");
                    
                    // Upload su Cloudinary
                    return uploadToCloudinary(base64Image, request.getPlatform());
                }
            }
            
            throw new RuntimeException("Nessuna immagine generata dall'API");
            
        } catch (Exception e) {
            log.error("❌ Errore Stability AI: {}", e.getMessage());
            throw new RuntimeException("Errore generazione immagine AI");
        }
    }
    
    /**
     * Upload su Cloudinary
     */
    private String uploadToCloudinary(String base64Image, String platform) {
        try {
            // Decodifica base64
            byte[] imageBytes = Base64.getDecoder().decode(base64Image);
            
            // Headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            
            ByteArrayResource resource = new ByteArrayResource(imageBytes) {
                @Override
                public String getFilename() {
                    return "social-post-" + platform + "-" + System.currentTimeMillis() + ".png";
                }
            };
            
            body.add("file", resource);
            body.add("upload_preset", cloudinaryUploadPreset);
            body.add("folder", "social_posts");
            body.add("tags", "ai_generated,social_media," + platform);
            
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            
            String uploadUrl = "https://api.cloudinary.com/v1_1/" + cloudinaryCloudName + "/image/upload";
            
            log.info("☁️ Upload a Cloudinary...");
            
            // Invia a Cloudinary
            ResponseEntity<Map> response = restTemplate.exchange(
                uploadUrl,
                HttpMethod.POST,
                requestEntity,
                Map.class
            );
            
            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("secure_url")) {
                String imageUrl = (String) responseBody.get("secure_url");
                log.info("✅ Upload completato: {}", imageUrl);
                return imageUrl;
            }
            
            throw new RuntimeException("Nessun URL dall'upload Cloudinary");
            
        } catch (Exception e) {
            log.error("❌ Errore Cloudinary: {}", e.getMessage());
            throw new RuntimeException("Errore upload immagine su Cloudinary");
        }
    }
}