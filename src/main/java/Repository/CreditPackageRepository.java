package Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import model.CreditPackage;

public interface CreditPackageRepository extends JpaRepository<CreditPackage, Long>{
List<CreditPackage> findByActiveTrue();
Optional <CreditPackage> findById(Long id);
Optional <CreditPackage> findByCodeAndActiveTrue(String code);
}
