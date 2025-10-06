package Service;


import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import Repository.UserRepository;
import model.User;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    public UserService(PasswordEncoder passwordEncoder) {
this.passwordEncoder = passwordEncoder;
}
    
    public User register(String email, String password) {
    	if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email già in uso");
        }
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setPlan("FREE");
        user.setCredits(10);
        return userRepository.save(user);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }
    
    public boolean matchesPassword(User user, String rawPassword) {
        return passwordEncoder.matches(rawPassword, user.getPasswordHash());
    }
    
    public void decrementCredits(User user) {
    	if (user.getCredits() == null) user.setCredits(0);
        if (user.getCredits() > 0) {
            user.setCredits(user.getCredits() - 1);
            userRepository.save(user);
        }
    }
}
