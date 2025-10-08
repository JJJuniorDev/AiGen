package Controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import DTO.BrandProfileDTO;
import DTO.UserDTO;
import Service.BrandProfileService;
import Service.UserService;
import model.BrandProfile;
import model.User;

@RestController
@RequestMapping("/api/brand-profiles")
public class BrandProfileController {

    @Autowired
    private BrandProfileService brandProfileService;
    
    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<List<BrandProfile>> getUserBrandProfiles(Authentication auth) {
        UserDTO principal = (UserDTO) auth.getPrincipal();
        List<BrandProfile> profiles = brandProfileService.findByUser(
            userService.findById(Long.parseLong(principal.getId())).orElseThrow()
        );
        return ResponseEntity.ok(profiles);
    }

    @PostMapping
    public ResponseEntity<BrandProfile> createBrandProfile(
            @RequestBody BrandProfileDTO dto, 
            Authentication auth) {
        UserDTO principal = (UserDTO) auth.getPrincipal();
        BrandProfile profile = brandProfileService.createForUser(dto, Long.parseLong(principal.getId()));
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BrandProfile> updateBrandProfile(
            @PathVariable Long id,
            @RequestBody BrandProfileDTO dto,
            Authentication auth) {
        UserDTO principal = (UserDTO) auth.getPrincipal();
        User user = userService.findById(Long.parseLong(principal.getId())).orElseThrow();
        
        // Verifica ownership
        Optional<BrandProfile> existing = brandProfileService.findByIdAndUser(id, user);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        BrandProfile updated = brandProfileService.updateBrandProfile(id, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBrandProfile(
            @PathVariable Long id,
            Authentication auth) {
        UserDTO principal = (UserDTO) auth.getPrincipal();
        User user = userService.findById(Long.parseLong(principal.getId())).orElseThrow();
        
        // Verifica ownership
        Optional<BrandProfile> existing = brandProfileService.findByIdAndUser(id, user);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        brandProfileService.deleteBrandProfile(id);
        return ResponseEntity.noContent().build();
    }
}