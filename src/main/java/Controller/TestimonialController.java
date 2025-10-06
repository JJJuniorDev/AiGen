package Controller;

import java.time.LocalDateTime;
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
import Service.LLMService;
import Service.TestimonialService;
import Service.UserService;
import mapper.DtoMapper;
import model.Testimonial;
import model.User;

@RestController
@RequestMapping("/api/testimonial")
public class TestimonialController {

    private final TestimonialService testimonialService;
    private final LLMService llmService;
    private final UserService userService;

    public TestimonialController(TestimonialService testimonialService,
                                 LLMService llmService,
                                 UserService userService) {
        this.testimonialService = testimonialService;
        this.llmService = llmService;
        this.userService = userService;
    }

    @PostMapping("/generate")
    public ResponseEntity<TestimonialDTO> generate(@RequestBody TestimonialDTO req) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) return ResponseEntity.status(401).build();

        UserDTO principal = (UserDTO) auth.getPrincipal();
        Optional<User> uOpt = userService.findById(Long.parseLong(principal.getId()));
        if (uOpt.isEmpty()) return ResponseEntity.status(401).build();

        User user = uOpt.get();
        // verifica crediti
        if (user.getCredits() == null || user.getCredits() <= 0) {
            return ResponseEntity.status(402).build(); // payment required
        }
        
        if (req.getPlatform() == null || req.getPlatform().isBlank())
            req.setPlatform("linkedin");

        // mock LLM call
        TestimonialDTO gen = llmService.generate(req.getInputText(), req.getPlatform(), 
        		req.getSelectedPostType(), req.getToneValue(), req.getStyleValue());
        
        Testimonial t = new Testimonial();
        t.setUser(user);
        t.setInputText(req.getInputText());
        t.setLinkedinPostVersions(gen.getLinkedinPostVersions());
        t.setHeadlineVersions(gen.getHeadlineVersions());
        t.setShortQuoteVersions(gen.getShortQuoteVersions());
        t.setCreatedAt(LocalDateTime.now());

        Testimonial saved = testimonialService.save(t);

        // decrementa crediti
        userService.decrementCredits(user);

        return ResponseEntity.ok(DtoMapper.toDTO(saved));
    }
}