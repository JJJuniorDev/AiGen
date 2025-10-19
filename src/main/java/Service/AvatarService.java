package Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import DTO.AvatarDTO;
import Repository.AvatarRepository;
import model.Avatar;

@Service
public class AvatarService {
    
    @Autowired
    private AvatarRepository avatarRepository;
    
    public List<AvatarDTO> getAllAvatars() {
        return avatarRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public List<AvatarDTO> getAvatarsByCategory(String category) {
        return avatarRepository.findByCategory(category).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public AvatarDTO getAvatarById(Long id) {
        return avatarRepository.findById(id)
                .map(this::convertToDTO)
                .orElseThrow(() -> new RuntimeException("Avatar non trovato"));
    }
    
    public Map<String, Object> getAvatarParameters(Long avatarId) {
        Avatar avatar = avatarRepository.findById(avatarId)
                .orElseThrow(() -> new RuntimeException("Avatar non trovato"));
        
        Map<String, Object> params = new HashMap<>();
        params.put("emotion", avatar.getDefaultEmotion());
        params.put("creativity", avatar.getDefaultCreativity());
        params.put("formality", avatar.getDefaultFormality());
        params.put("urgency", avatar.getDefaultUrgency());
        params.put("length", avatar.getDefaultLength());
        params.put("tone", avatar.getDefaultTone());
        params.put("characteristicPhrases", avatar.getCharacteristicPhrases());
        
        return params;
    }
    
    private AvatarDTO convertToDTO(Avatar avatar) {
        AvatarDTO dto = new AvatarDTO();
        dto.setId(avatar.getId());
        dto.setName(avatar.getName());
        dto.setDescription(avatar.getDescription());
        dto.setIcon(avatar.getIcon());
        dto.setDefaultEmotion(avatar.getDefaultEmotion());
        dto.setDefaultCreativity(avatar.getDefaultCreativity());
        dto.setDefaultFormality(avatar.getDefaultFormality());
        dto.setDefaultUrgency(avatar.getDefaultUrgency());
        dto.setDefaultLength(avatar.getDefaultLength());
        dto.setDefaultTone(avatar.getDefaultTone());
        dto.setCategory(avatar.getCategory());
        dto.setBiography(avatar.getBiography());
        dto.setCharacteristicPhrases(avatar.getCharacteristicPhrases());
        return dto;
    }
}