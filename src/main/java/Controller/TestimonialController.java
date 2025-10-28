package Controller;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import DTO.TestimonialDTO;
import DTO.UserDTO;
import Service.BrandProfileService;
import Service.LLMService;
import Service.TestimonialService;
import Service.UserService;
import mapper.DtoMapper;
import model.BrandProfile;
import model.Testimonial;
import model.User;

@RestController
@RequestMapping("/api/testimonial")
public class TestimonialController {

    private final TestimonialService testimonialService;
    private final LLMService llmService;
    private final UserService userService;
    private final BrandProfileService brandService;
    
    public TestimonialController(TestimonialService testimonialService,
                                 LLMService llmService,
                                 UserService userService,
                                 BrandProfileService brandService) {
        this.testimonialService = testimonialService;
        this.llmService = llmService;
        this.userService = userService;
        this.brandService= brandService;
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generate(@RequestBody TestimonialDTO req) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) return ResponseEntity.status(401).build();

        UserDTO principal = (UserDTO) auth.getPrincipal();
        Optional<User> uOpt = userService.findById(Long.parseLong(principal.getId()));
        if (uOpt.isEmpty()) return ResponseEntity.status(401).build();

        User user = uOpt.get();
        // ✅ VERIFICA CREDITI AGGIORNATA
        if (!userService.useCredit(user, req.getSelectedPostType())) {
            return ResponseEntity.status(402).body(createNoCreditsResponse());
        }
        
        if (req.getPlatform() == null || req.getPlatform().isBlank())
            req.setPlatform("linkedin");

        // ✅ NUOVO: Recupera Brand Profile se specificato
        BrandProfile brandProfile = null;
        if (req.getBrandProfileId() != null) {
        	 try {
                 Long brandId = Long.valueOf(req.getBrandProfileId());
                 Optional<BrandProfile> brandProfileOpt  = brandService.findByIdAndUser(brandId, user);
                 if (brandProfileOpt.isEmpty()) {
                	    return ResponseEntity.notFound().build();
                	}
                	 brandProfile = brandProfileOpt.get();
        	 } catch (NumberFormatException e) {
                 return ResponseEntity.badRequest().build();
             }
            // Verifica che il brand appartenga all'utente
            if (!brandProfile.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(403).build();
            }
        }
        try {
        // mock LLM call
        TestimonialDTO gen = llmService.generate(
        		req.getInputText(),
        		req.getPlatform(), 
        		req.getSelectedPostType(),
        		req.getEmotion(),
        		req.getCreativity(),
        		req.getFormality(),
        		req.getUrgency(),
        		req.getLength(),
        		brandProfile);
        
        Testimonial t = new Testimonial();
        t.setUser(user);
        t.setInputText(req.getInputText());
        t.setSocialPostVersions(gen.getSocialPostVersions());
        t.setHeadlineVersions(gen.getHeadlineVersions());
        t.setShortQuoteVersions(gen.getShortQuoteVersions());
        t.setCallToActionVersions(gen.getCallToActionVersions());
        t.setCreatedAt(LocalDateTime.now());

        Testimonial saved = testimonialService.save(t);


        return ResponseEntity.ok(DtoMapper.toDTO(saved));
    } catch (RuntimeException e) {
    	   // ✅ GESTIONE SPECIFICA PER SERVIZIO OCCUPATO
        if (e.getMessage().contains("Servizio temporaneamente occupato")) {
            return ResponseEntity.status(429) // 429 Too Many Requests
                .body(Map.of(
                    "error", "service_busy",
                    "message", "Il servizio è temporaneamente occupato",
                    "retry_after", 30,
                    "suggestion", "Riprova tra 30 secondi"
                ));
        }
         // ✅ ALTRI ERRORI
            return ResponseEntity.status(503)
                .body(Map.of(
                    "error", "generation_failed", 
                    "message", "Errore durante la generazione del contenuto"
                ));
        
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