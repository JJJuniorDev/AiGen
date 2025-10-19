package Controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import DTO.UserDTO;
import Service.UserService;
import model.User;

@RestController
@RequestMapping("/api/users")
public class UserController {
    
	@Autowired
	private  UserService userService;
	
    @GetMapping("/me")
    public ResponseEntity<User> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            return ResponseEntity.status(401).build();
        }

        UserDTO principal = (UserDTO) auth.getPrincipal();
        Optional<User> userOpt = userService.findById(Long.parseLong(principal.getId()));
        
        return userOpt.map(ResponseEntity::ok)
                     .orElse(ResponseEntity.status(401).build());
    }
}