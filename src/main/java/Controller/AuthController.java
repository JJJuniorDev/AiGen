package Controller;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import Config.JwtUtil;
import DTO.AuthRequest;
import DTO.AuthResponse;
import DTO.UserDTO;
import Service.UserService;
import mapper.DtoMapper;
import model.User;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    public AuthController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/signup")
    public ResponseEntity<UserDTO> signup(@RequestBody AuthRequest req) {
        try {
            User u = userService.register(req.getEmail(), req.getPassword());
            return ResponseEntity.ok(DtoMapper.toDTO(u));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest req) {
        Optional<User> o = userService.findByEmail(req.getEmail());
        if (o.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        User user = o.get();
        if (!userService.matchesPassword(user, req.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String token = jwtUtil.generateToken(user.getId().toString());
        UserDTO dto = DtoMapper.toDTO(user);
        return ResponseEntity.ok(new AuthResponse(token, dto));
    }
}