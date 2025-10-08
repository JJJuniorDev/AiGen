package Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import model.BrandProfile;
import model.User;

public interface BrandProfileRepository extends JpaRepository<BrandProfile, Long>{
List<BrandProfile> findByUser(User user);
Optional<BrandProfile> findByIdAndUser(Long id, User user);
}
