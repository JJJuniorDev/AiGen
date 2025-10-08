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
		// TODO Auto-generated method stub
		return null;
	}

	public void deleteBrandProfile(Long id) {
		// TODO Auto-generated method stub
		
	}
}