package Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import model.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    
 // ✅ Prova con una query esplicita
  //  @Query("SELECT u FROM User u WHERE u.email = :email")
    // AGGIUNGI QUESTO
    @Query(value = "SELECT id, email, credits, plan, password_hash, created_at, updated_at, max_brands, active, brand_logo_url, primary_color, stripe_customer_id FROM users WHERE email = :email", nativeQuery = true)
    Optional<User> findByEmailWithAllFields(@Param("email") String email);
}
