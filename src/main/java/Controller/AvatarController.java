package Controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import DTO.AvatarDTO;
import Service.AvatarService;

@RestController
@RequestMapping("/api/avatars")
public class AvatarController {
    
    @Autowired
    private AvatarService avatarService;
    
    @GetMapping
    public ResponseEntity<List<AvatarDTO>> getAllAvatars() {
        return ResponseEntity.ok(avatarService.getAllAvatars());
    }
    
    @GetMapping("/category/{category}")
    public ResponseEntity<List<AvatarDTO>> getAvatarsByCategory(@PathVariable String category) {
        return ResponseEntity.ok(avatarService.getAvatarsByCategory(category));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<AvatarDTO> getAvatarById(@PathVariable Long id) {
        return ResponseEntity.ok(avatarService.getAvatarById(id));
    }
    
    @GetMapping("/{id}/parameters")
    public ResponseEntity<Map<String, Object>> getAvatarParameters(@PathVariable Long id) {
        return ResponseEntity.ok(avatarService.getAvatarParameters(id));
    }
}