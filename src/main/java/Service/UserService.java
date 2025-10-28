package Service;


import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import Repository.UserCreditTransactionRepository;
import Repository.UserRepository;
import model.User;
import model.UserCreditTransaction;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserCreditTransactionRepository transactionRepository;
    
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
        user.setCredits(5);
        user.setMaxBrands(1); //FREE PLAN LIMIT
 User savedUser = userRepository.save(user);
        
        // Registra i crediti iniziali come bonus
        createTransaction(savedUser, "BONUS", 5, 5, "Crediti trial iniziali");
        return savedUser;
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmailWithAllFields(email);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }
    
    public boolean matchesPassword(User user, String rawPassword) {
        return passwordEncoder.matches(rawPassword, user.getPasswordHash());
    }
    
 // ✅ NUOVO: Metodo robusto per usare crediti
    public boolean useCredit(User user, String operationType) {
        if (user.getCredits() == null || user.getCredits() <= 0) {
            return false;
        }
        
        int oldBalance = user.getCredits();
        user.setCredits(user.getCredits() - 1);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        
        // Registra la transazione
        createTransaction(user, "USAGE", -1, user.getCredits(), 
                         "Generazione contenuto: " + operationType);
        
        return true;
    }
    
    // ✅ NUOVO: Aggiungi crediti (per acquisti)
    public void addCredits(User user, Integer credits, String description, String referenceId) {
        if (user.getCredits() == null) {
            user.setCredits(0);
        }
        
        user.setCredits(user.getCredits()+ credits);
        userRepository.save(user);
        
        createTransaction(user, "PURCHASE", credits, user.getCredits(), 
                         description, referenceId);
    }
    
    // ✅ NUOVO: Verifica se può creare brand
    public boolean canCreateBrand(User user, long currentBrandCount) {
        return currentBrandCount < user.getMaxBrands();
    }
    
    // ✅ NUOVO: Aggiorna piano utente
    public void updatePlan(User user, String newPlan) {
        user.setPlan(newPlan);
        user.updateMaxBrandsBasedOnPlan();
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }
    
 // ✅ NUOVO: Crea transazione crediti
    private void createTransaction(User user, String type, Integer creditChange, 
                                  Integer balanceAfter, String description) {
        createTransaction(user, type, creditChange, balanceAfter, description, null);
    }
    
    private void createTransaction(User user, String type, Integer creditChange, 
            Integer balanceAfter, String description, String referenceId) {
UserCreditTransaction transaction = new UserCreditTransaction();
transaction.setUser(user);
transaction.setType(type);
transaction.setCreditChange(creditChange);
transaction.setBalanceAfter(balanceAfter);
transaction.setDescription(description);
transaction.setReferenceId(referenceId);
transactionRepository.save(transaction);
}
    
   /* public void decrementCredits(User user) {
    	if (user.getCredits() == null) user.setCredits(0);
        if (user.getCredits() > 0) {
            user.setCredits(user.getCredits() - 1);
            userRepository.save(user);
        }
    }*/
}
