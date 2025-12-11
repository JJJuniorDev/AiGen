package Service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import DTO.SocialImageBatchRequest;
import DTO.SocialImageRequest;
import DTO.SocialImageResponse;

@Service
public class ImageGenerationService {

    private static final Logger log = LoggerFactory.getLogger(ImageGenerationService.class);
    
    private static final Map<String, Double> EDIT_STRENGTH_MAP = Map.of(
    	    "subtle", 0.65,      // Modifica sottile
    	    "normal", 0.35,      // Modifica normale (default)
    	    "strong", 0.15,      // Modifica forte
    	    "transform", 0.1     // Trasformazione radicale
    	);

    	private static final Set<String> VALID_STYLES = Set.of(
    	    "3d-model", "analog-film", "anime", "cinematic", "comic-book",
    	    "digital-art", "enhance", "fantasy-art", "isometric", "line-art",
    	    "low-poly", "modeling-compound", "neon-punk", "origami", "photographic",
    	    "pixel-art", "tile-texture"
    	);
    	
    @Value("${stability.api.key:}")
    private String stabilityApiKey;
    
    @Value("${cloudinary.cloud.name:}")
    private String cloudinaryCloudName;
    
    @Value("${cloudinary.upload.preset:}")
    private String cloudinaryUploadPreset;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    // ===================================================================
    // METODI PRINCIPALI DI GENERAZIONE
    // ===================================================================
    
    /**
     * Genera una nuova immagine da testo (text-to-image)
     */
    public SocialImageResponse generateSocialImage(SocialImageRequest request) {
        log.info("🎨 Generazione NUOVA immagine per {}", request.getBrandName());
        log.info("📝 Prompt ricevuto (ITALIANO): {}", request.getPrompt());
        
        try {
        	
        	  String englishPrompt = translateItalianToEnglish(request.getPrompt());
              log.info("🌐 Prompt tradotto (INGLESE): {}", englishPrompt);
              request.setPrompt(englishPrompt);
            // 1. Arricchisci il prompt con dettagli tecnici
            String enhancedPrompt = enhancePromptForAI(request);
            log.info("✨ Prompt arricchito: {}", enhancedPrompt);
            
            // 2. Genera immagine AI in modalità text-to-image
            String imageBase64 = generateImageFromText(
                enhancedPrompt, 
                request.getPlatform(),
                request.getStyle()
            );
            
            // 3. Crea e restituisci risposta
            return createResponse(
                request, 
                imageBase64, 
                enhancedPrompt, 
                false, 
                null
            );
            
        } catch (Exception e) {
            log.error("❌ Errore generazione immagine: {}", e.getMessage(), e);
            throw new RuntimeException("Impossibile generare l'immagine: " + e.getMessage());
        }
    }
    
    /**
     * Modifica un'immagine esistente (image-to-image)
     */
    public SocialImageResponse editSocialImage(SocialImageRequest request) {
        log.info("✏️ Modifica immagine esistente per {}", request.getBrandName());
        log.info("📝 Nuovo prompt (ITALIANO): {}", request.getPrompt());
        
        try {
            // Validazione: deve esserci un'immagine base da modificare
            if (request.getBaseImage() == null || request.getBaseImage().isEmpty()) {
                throw new IllegalArgumentException("Base image is required for editing");
            }
            
            String englishPrompt = translateItalianToEnglish(request.getPrompt());
            log.info("🌐 Prompt tradotto per modifica (INGLESE): {}", englishPrompt);
            request.setPrompt(englishPrompt);
            
            // 1. Arricchisci il prompt con dettagli tecnici
            String enhancedPrompt = enhancePromptForAI(request);
            log.info("✨ Prompt modifica arricchito: {}", enhancedPrompt);
            
            Double imageStrength = determineOptimalImageStrength(request.getPrompt(), enhancedPrompt);
            log.info("⚙️ Image strength ottimale: {} (calcolato automaticamente)", imageStrength);
            
            // 2. Decodifica l'immagine base64 esistente
            byte[] initImageBytes = Base64.getDecoder()
                .decode(request.getBaseImage().replaceFirst("data:image/[^;]+;base64,", ""));
            
            log.info("🖼️ Immagine base ricevuta: {} bytes", initImageBytes.length);
           
            // 4. Genera immagine in modalità image-to-image
            String imageBase64 = generateImageFromImage(
                enhancedPrompt,
                initImageBytes,
                request.getPlatform(),
                request.getStyle(),
                imageStrength
            );
            
            // 5. Incrementa il contatore modifiche
            Integer editCount = request.getEditCount() != null 
                              ? request.getEditCount() + 1 
                              : 1;
            
            // 6. Crea e restituisci risposta
            SocialImageResponse response = createResponse(
                request, 
                imageBase64, 
                enhancedPrompt, 
                true, 
                editCount
            );
            
            log.info("✅ Immagine modificata (Iterazione #{}, strength: {})", response.getEditCount(), imageStrength);
            
            return response;
            
        } catch (Exception e) {
            log.error("❌ Errore modifica immagine: {}", e.getMessage(), e);
            throw new RuntimeException("Impossibile modificare l'immagine: " + e.getMessage());
        }
    }
    
    // ===================================================================
    // METODI PER BATCH GENERATION (per compatibilità)
    // ===================================================================
    
    public List<SocialImageResponse> generateBatchImages(SocialImageBatchRequest request) {
        log.info("🎨 Generazione batch di {} immagini per {}", 
                 request.getPosts().size(), request.getBrandName());
        
        List<SocialImageResponse> images = new ArrayList<>();
        
        for (int i = 0; i < request.getPosts().size(); i++) {
            try {
                String postContent = request.getPosts().get(i);
                
                SocialImageRequest singleRequest = new SocialImageRequest();
                singleRequest.setPrompt(postContent); // Usa setPrompt invece di setContent
                singleRequest.setPlatform(request.getPlatform());
                singleRequest.setBrandName(request.getBrandName());
                singleRequest.setStyle(request.getStyle());
                
                SocialImageResponse response = generateSocialImage(singleRequest);
                images.add(response);
                
                log.info("✅ Immagine {} generata (Base64)", i + 1);
                
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
    
    // ===================================================================
    // METODI PRIVATI DI SUPPORTO
    // ===================================================================
    
    /**
     * Arricchisce il prompt con dettagli tecnici per migliorare i risultati AI
     */
    private String enhancePromptForAI(SocialImageRequest request) {
        StringBuilder promptBuilder = new StringBuilder();
        
        // 1. Prompt principale dall'utente (OBBLIGATORIO)
        promptBuilder.append(request.getPrompt());
        
        String style = request.getStyle().toLowerCase();
        
        if (!VALID_STYLES.contains(style)) {
            log.warn("⚠️ Stile '{}' non valido, uso 'cinematic' come fallback", style);
            style = "cinematic";
        }
        
        // 2. Aggiungi stile se specificato
       
            promptBuilder.append(", ").append(request.getStyle()).append(" style");
        
        
        // 3. Aggiungi ottimizzazione per piattaforma
        promptBuilder.append(", optimized for ").append(request.getPlatform()).append(" social media");
        
        // 4. Qualità e dettagli tecnici (fissi)
        promptBuilder.append(", high quality, detailed, professional");
        
        return promptBuilder.toString();
    }
    
    /**
     * Genera un'immagine da testo (text-to-image)
     */
    private String generateImageFromText(String prompt, String platform, String style) {
        try {
            HttpHeaders headers = createStabilityHeaders();
            
            Map<String, Object> requestBody = createBaseRequestBody(prompt, platform);
            
            // Imposta stile se specificato
            if (style != null && !style.isEmpty()) {
                String stylePreset = style.toLowerCase();
                stylePreset =stylePreset;
                if (VALID_STYLES.contains(stylePreset)) {
                    requestBody.put("style_preset", stylePreset);
                    log.info("🎨 Stile applicato: {}", stylePreset);
                } else {
                    log.warn("⚠️ Stile '{}' non supportato, uso default", stylePreset);
                }
            }
            
            
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
            
            log.info("🔄 Text-to-image a Stability AI...");
            
            ResponseEntity<Map> response = restTemplate.exchange(
                "https://api.stability.ai/v1/generation/stable-diffusion-xl-1024-v1-0/text-to-image",
                HttpMethod.POST,
                requestEntity,
                Map.class
            );
            
            return extractBase64FromResponse(response);
            
        } catch (Exception e) {
            log.error("❌ Errore text-to-image: {}", e.getMessage());
            throw new RuntimeException("Errore generazione immagine da testo: " + e.getMessage());
        }
    }
    
    private String generateImageFromImage(String prompt, byte[] initImageBytes, 
            String platform, String style, 
            double imageStrength) {
        try {
            // 1. HEADERS per multipart/form-data
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set("Authorization", "Bearer " + stabilityApiKey);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            // 2. Creazione del body multipart
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

            byte[] processedImageBytes = compressImageIfNeeded(initImageBytes, 5_200_000L);
            
            // Parte 1: L'immagine come file
            ByteArrayResource imageResource = new ByteArrayResource(processedImageBytes) {
                @Override
                public String getFilename() {
                    return "init-image.png";
                }
            };
            body.add("init_image", imageResource);

            // Parte 2: PARAMETRI PRINCIPALI
            // Per image-to-image, i parametri vanno inviati come campi separati, NON come JSON annidato
            
            // Aggiungi il prompt direttamente come stringa
            body.add("text_prompts[0][text]", prompt);
            body.add("text_prompts[0][weight]", "1.0");
            
            // Prompt negativo opzionale (ma consigliato)
            body.add("text_prompts[1][text]", "ugly, blurry, low quality, text, watermark, signature");
            body.add("text_prompts[1][weight]", "-1.0");

            // Parte 3: Altri parametri richiesti
          
            body.add("image_strength", String.valueOf(imageStrength));
            body.add("cfg_scale", "7");
            body.add("samples", "1");
            body.add("steps", "30");

            // Stile se specificato
            if (style != null && !style.isEmpty()) {
                body.add("style_preset", style.toLowerCase());
            }

            // Log dettagliato
            log.info("🔄 Image-to-image a Stability AI (strength: {})...", imageStrength);
            log.info("📤 Dimensione immagine: {} bytes", initImageBytes.length);
            log.info("📝 Prompt inviato: {}", prompt);
       
            log.info("🔧 Parametri: cfg_scale=7, steps=30, samples=1");

            // 3. Crea la richiesta
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // 4. Invia la richiesta
            ResponseEntity<Map> response = restTemplate.exchange(
                "https://api.stability.ai/v1/generation/stable-diffusion-xl-1024-v1-0/image-to-image",
                HttpMethod.POST,
                requestEntity,
                Map.class
            );

            return extractBase64FromResponse(response);

        } catch (Exception e) {
            log.error("❌ Errore image-to-image: {}", e.getMessage(), e);
            throw new RuntimeException("Errore modifica immagine: " + e.getMessage());
        }
    }
    
    /**
     * Crea gli headers per le chiamate a Stability AI
     */
    private HttpHeaders createStabilityHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + stabilityApiKey);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return headers;
    }
    
    /**
     * Crea il body base per le richieste a Stability AI
     */
    private Map<String, Object> createBaseRequestBody(String prompt, String platform) {
        Map<String, Object> requestBody = new HashMap<>();
        
        // Prompts
        List<Map<String, Object>> textPrompts = new ArrayList<>();
        
        Map<String, Object> positivePrompt = new HashMap<>();
        positivePrompt.put("text", prompt);
        positivePrompt.put("weight", 1.0);
        textPrompts.add(positivePrompt);
        
        Map<String, Object> negativePrompt = new HashMap<>();
        negativePrompt.put("text", "ugly, blurry, low quality, text, watermark, signature, letters, words, deformed");
        negativePrompt.put("weight", -1.0);
        textPrompts.add(negativePrompt);
        
        requestBody.put("text_prompts", textPrompts);
        
        // Dimensioni
        Map<String, Integer> dimensions = getDimensionsForPlatform(platform);
        requestBody.put("width", dimensions.get("width"));
        requestBody.put("height", dimensions.get("height"));
        
        // Parametri qualità
        requestBody.put("cfg_scale", 7);
        requestBody.put("samples", 1);
        requestBody.put("steps", 30);
        
        return requestBody;
    }
    
    /**
     * Estrae l'immagine Base64 dalla risposta di Stability AI
     */
    private String extractBase64FromResponse(ResponseEntity<Map> response) {
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
    }
    
    /**
     * Crea l'oggetto di risposta standard
     */
    private SocialImageResponse createResponse(SocialImageRequest request, 
                                             String imageBase64, 
                                             String promptUsed,
                                             boolean isEdit,
                                             Integer editCount) {
        SocialImageResponse response = new SocialImageResponse();
        response.setImageBase64(imageBase64);
        response.setImageUrl(null); // Cloudinary URL vuoto (upload opzionale)
        response.setPromptUsed(promptUsed);
        response.setPlatform(request.getPlatform());
        response.setDimensions(getDimensionsForPlatform(request.getPlatform()));
        response.setGeneratedAt(new Date());
        response.setSavedToCloudinary(false);
        response.setTemporaryId(UUID.randomUUID().toString());
        response.setIsEdit(isEdit);
        response.setEditCount(editCount);
        
        return response;
    }
    
    // ===================================================================
    // METODI PER CLOUDINARY (UPLOAD OPZIONALE)
    // ===================================================================
    
    /**
     * Salva un'immagine Base64 su Cloudinary (su richiesta esplicita)
     */
    public SocialImageResponse saveImageToCloudinary(String imageBase64, String platform, String brandName) {
        try {
            byte[] imageBytes = Base64.getDecoder().decode(imageBase64);
            
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
    
    // ===================================================================
    // METODI UTILITY
    // ===================================================================
    
    /**
     * Restituisce le dimensioni ottimali per ogni piattaforma
     */
    private Map<String, Integer> getDimensionsForPlatform(String platform) {
        Map<String, Integer> dimensions = new HashMap<>();
        
        switch (platform.toLowerCase()) {
            case "linkedin":
                dimensions.put("width", 1216);
                dimensions.put("height", 832);
                break;
            case "twitter":
                dimensions.put("width", 1344);
                dimensions.put("height", 768);
                break;
            case "facebook":
                dimensions.put("width", 1216);
                dimensions.put("height", 832);
                break;
            case "instagram":
            default:
                dimensions.put("width", 1024);
                dimensions.put("height", 1024);
                break;
        }
        
        log.debug("Dimensioni per {}: {}x{}", platform, dimensions.get("width"), dimensions.get("height"));
        
        return dimensions;
    }
    
    private String translateItalianToEnglish(String italianText) {
        try {
            log.info("🌐 Traduzione italiano → inglese: {}", 
                     italianText.substring(0, Math.min(50, italianText.length())));
            
            // Limita a 500 caratteri (limite MyMemory)
            String textToTranslate = italianText.length() > 500 
                ? italianText.substring(0, 500) 
                : italianText;
            
            // Costruisci URL per MyMemory
            String apiUrl = "https://api.mymemory.translated.net/get" +
                "?q=" + java.net.URLEncoder.encode(textToTranslate, "UTF-8") +
                "&langpair=it|en" +
                "&de=lorenzo.detoma3@gmail.com"; // 👈 Tua email per 50k caratteri
            
            // Chiama MyMemory
            ResponseEntity<Map> response = restTemplate.getForEntity(apiUrl, Map.class);
            Map<String, Object> responseBody = response.getBody();
            
            if (responseBody != null && 
                "200".equals(String.valueOf(responseBody.get("responseStatus"))) &&
                responseBody.get("responseData") != null) {
                
                Map<String, Object> responseData = (Map<String, Object>) responseBody.get("responseData");
                String translatedText = (String) responseData.get("translatedText");
                
                if (translatedText != null && !translatedText.isEmpty()) {
                    log.info("✅ Traduzione completata: {} → {}", 
                             textToTranslate.substring(0, Math.min(30, textToTranslate.length())),
                             translatedText.substring(0, Math.min(30, translatedText.length())));
                    return translatedText;
                }
            }
            
            log.warn("⚠️ Traduzione fallita, uso testo originale");
            return italianText;
            
        } catch (Exception e) {
            log.error("❌ Errore traduzione: {}", e.getMessage());
            return italianText; // Fallback a italiano
        }
    }
    
    private byte[] compressImageIfNeeded(byte[] imageBytes, long maxSizeBytes) throws IOException {
        long currentSize = imageBytes.length;
        
        if (currentSize <= maxSizeBytes) {
            log.info("✅ Immagine entro il limite ({} bytes)", currentSize);
            return imageBytes;
        }

        log.warn("⚠️ Immagine troppo grande ({} bytes). Provo a comprimere...", currentSize);
        
        // 1. Decodifica i bytes in un BufferedImage
        ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
        BufferedImage image = ImageIO.read(bais);
        
        // Riduci le dimensioni se necessario (mantenendo aspect ratio)
        if (image.getWidth() > 1024 || image.getHeight() > 1024) {
            int newWidth = Math.min(image.getWidth(), 1024);
            int newHeight = (int) ((double) newWidth / image.getWidth() * image.getHeight());
            
            BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g2d = resized.createGraphics();
            g2d.drawImage(image.getScaledInstance(newWidth, newHeight, java.awt.Image.SCALE_SMOOTH), 0, 0, null);
            g2d.dispose();
            image = resized;
            log.info("📐 Immagine ridimensionata a {}x{}", newWidth, newHeight);
        }
        
        // Compressione ottimizzata
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        
        byte[] optimizedBytes = baos.toByteArray();
        log.info("✅ Dopo ottimizzazione: {} bytes (riduzione: {}%)", 
                 optimizedBytes.length, 
                 100 - (optimizedBytes.length * 100 / currentSize));
        
        return optimizedBytes;
    }
    
    
    
    private Double determineOptimalImageStrength(String originalPrompt, String enhancedPrompt) {
        // Analizza il prompt per capire se richiede cambiamenti radicali
        String promptLower = enhancedPrompt.toLowerCase();
        
        // Parole chiave che indicano cambiamenti radicali
        Set<String> radicalKeywords = Set.of(
            "add", "remove", "change", "transform", "completely", "totally",
            "instead", "different", "new", "create", "make it", "turn into"
        );
        
        // Parole chiave che indicano modifiche sottili
        Set<String> subtleKeywords = Set.of(
            "adjust", "slightly", "a bit", "little", "enhance", "improve",
            "refine", "touch up", "minor", "small"
        );
        
        int radicalCount = 0;
        int subtleCount = 0;
        
        for (String keyword : radicalKeywords) {
            if (promptLower.contains(keyword)) radicalCount++;
        }
        
        for (String keyword : subtleKeywords) {
            if (promptLower.contains(keyword)) subtleCount++;
        }
        
        // Logica per determinare la forza
        if (radicalCount > subtleCount && radicalCount >= 2) {
            return 0.15; // Modifica forte per cambiamenti radicali
        } else if (subtleCount > radicalCount) {
            return 0.65; // Modifica sottile
        }
        
        return 0.35; // Default
    }
    
    
  
}