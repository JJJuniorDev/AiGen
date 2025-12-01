package Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import DTO.BrandProfileDTO;
import Repository.BrandProfileRepository;
import model.BrandProfile;
import model.User;

@Service
public class BrandProfileService {

	@Autowired
    private BrandProfileRepository brandRepository;

    public Optional<BrandProfile> findById(Long id) {
        return brandRepository.findById(id);
    }

    public Optional<BrandProfile> findByIdAndUser(Long id, User user) {
        return brandRepository.findByIdAndUser(id, user);
    }

    public List<BrandProfile> findByUser(User user) {
        return brandRepository.findByUser(user);
    }

    public BrandProfile save(BrandProfile brandProfile) {
        brandProfile.setUpdatedAt(LocalDateTime.now());
        return brandRepository.save(brandProfile);
    }

    public BrandProfile createForUser(BrandProfileDTO dto, Long userId) {
    	User user = new User();
        user.setId(userId); 
    	long currentBrandsCount = brandRepository.countByUser(user);
           if (currentBrandsCount >= user.getMaxBrands()) {
               throw new RuntimeException(
                   "Limite brand raggiunto! Hai " + currentBrandsCount + 
                   " brand su " + user.getMaxBrands() + " disponibili."
               );
           }
    	

        BrandProfile profile = new BrandProfile();
        profile.setUser(user);
        profile.setBrandName(dto.getBrandName());
        profile.setTone(dto.getTone());
        profile.setPreferredKeywords(dto.getPreferredKeywords());
        profile.setAvoidedWords(dto.getAvoidedWords());
        profile.setBrandDescription(dto.getBrandDescription());
        profile.setTargetAudience(dto.getTargetAudience());
        profile.setBrandValues(dto.getBrandValues());
        profile.setTagline(dto.getTagline());
        profile.setDefaultHashtags(dto.getDefaultHashtags());
        profile.setVisualStyle(dto.getVisualStyle());
        profile.setColorPalette(dto.getColorPalette());
        profile.setCreatedAt(LocalDateTime.now());
        profile.setUpdatedAt(LocalDateTime.now());

        return brandRepository.save(profile);
    }

    public BrandProfile updateBrandProfile(Long id, BrandProfileDTO dto) {
        BrandProfile existing = brandRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Brand non trovato"));
        
        // Aggiorna i campi
        existing.setBrandName(dto.getBrandName());
        existing.setTone(dto.getTone());
        existing.setPreferredKeywords(dto.getPreferredKeywords());
        existing.setAvoidedWords(dto.getAvoidedWords());
        existing.setBrandDescription(dto.getBrandDescription());
        existing.setTargetAudience(dto.getTargetAudience());
        existing.setBrandValues(dto.getBrandValues());
        existing.setTagline(dto.getTagline());
        existing.setDefaultHashtags(dto.getDefaultHashtags());
        existing.setVisualStyle(dto.getVisualStyle());
        existing.setColorPalette(dto.getColorPalette());
        existing.setUpdatedAt(LocalDateTime.now());
        
        return brandRepository.save(existing);
    }

    public void deleteBrandProfile(Long id) {
        if (!brandRepository.existsById(id)) {
            throw new RuntimeException("Brand non trovato");
        }
        brandRepository.deleteById(id);
    }
    
    public long countByUser(User user) {
        return brandRepository.countByUser(user);
    }
}