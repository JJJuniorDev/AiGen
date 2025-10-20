package Controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import Service.StripeService;
import Service.CreditPackageService;
import Service.UserService;
import model.CreditPackage;
import model.User;
import DTO.UserDTO;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    
    @Autowired
    private StripeService stripeService;
    
    @Autowired
    private CreditPackageService creditPackageService;
    
    @Autowired
    private UserService userService;

    @PostMapping("/create-checkout-session")
    public ResponseEntity<?> createCheckoutSession(
            @RequestBody CheckoutRequest request,
            Authentication auth) {
        
        try {
            UserDTO principal = (UserDTO) auth.getPrincipal();
            User user = userService.findById(Long.parseLong(principal.getId()))
                .orElseThrow(() -> new RuntimeException("User not found"));

            CreditPackage creditPackage = creditPackageService.findById(request.getPackageId())
                .orElseThrow(() -> new RuntimeException("Credit package not found"));

            if (!creditPackage.getActive()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Package not available"));
            }

            String checkoutUrl = stripeService.createCheckoutSession(user, creditPackage);
            
            // ✅ CORREZIONE: NON aggiungere i crediti qui!
            // I crediti vanno aggiunti SOLO dopo che il pagamento è confermato via webhook
            // Questo metodo crea solo la sessione di checkout, non processa il pagamento
            
            return ResponseEntity.ok(Map.of(
                "checkoutUrl", checkoutUrl,
                "packageName", creditPackage.getName(),
                "price", creditPackage.getPrice()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Error creating checkout session: " + e.getMessage()));
        }
    }

    @PostMapping("/webook")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        
        return stripeService.handleWebhook(payload, sigHeader);
    }

    // ✅ DTO come classe statica interna
    public static class CheckoutRequest {
        private Long packageId;
        private String successUrl;
        private String cancelUrl;

        // getters/setters
        public Long getPackageId() { return packageId; }
        public void setPackageId(Long packageId) { this.packageId = packageId; }
        public String getSuccessUrl() { return successUrl; }
        public void setSuccessUrl(String successUrl) { this.successUrl = successUrl; }
        public String getCancelUrl() { return cancelUrl; }
        public void setCancelUrl(String cancelUrl) { this.cancelUrl = cancelUrl; }
    }
}