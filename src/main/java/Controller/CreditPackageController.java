package Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import Service.CreditPackageService;
import model.CreditPackage;

@RestController
@RequestMapping("/api/credit-packages")
public class CreditPackageController {
    
    @Autowired
    private CreditPackageService creditPackageService;
    
    @GetMapping
    public ResponseEntity<List<CreditPackage>> getActivePackages() {
        return ResponseEntity.ok(creditPackageService.getActivePackages());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<CreditPackage> getPackage(@PathVariable Long id) {
        return creditPackageService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}