package Service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import Repository.CreditPackageRepository;
import model.CreditPackage;

@Service
public class CreditPackageService {
    
    @Autowired
    private CreditPackageRepository creditPackageRepository;
    
    public List<CreditPackage> getActivePackages() {
        return creditPackageRepository.findByActiveTrue();
    }
    
    public Optional<CreditPackage> findById(Long id) {
        return creditPackageRepository.findById(id);
    }
    
    public Optional<CreditPackage> findByCode(String code) {
        return creditPackageRepository.findByCodeAndActiveTrue(code);
    }
    
    // ✅ AGGIUNGI QUESTO METODO MANCANTE!
    public CreditPackage save(CreditPackage creditPackage) {
        return creditPackageRepository.save(creditPackage);
    }
}