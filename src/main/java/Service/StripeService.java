package Service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.model.Event;
import com.stripe.model.Product;
import com.stripe.model.Price;
import com.stripe.param.ProductCreateParams;
import com.stripe.param.PriceCreateParams;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import model.CreditPackage;
import model.User;

@Service
public class StripeService {
    
    @Value("${stripe.secret.key}")
    private String stripeSecretKey;
    
    @Value("${stripe.webhook.secret}")
    private String webhookSecret;
    
    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private CreditPackageService creditPackageService;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    public String createCheckoutSession(User user, CreditPackage creditPackage) throws StripeException {
        
       

        SessionCreateParams params = SessionCreateParams.builder()
            .setMode(SessionCreateParams.Mode.PAYMENT)
            .setSuccessUrl(frontendUrl + "/payment/success?session_id={CHECKOUT_SESSION_ID}")
            .setCancelUrl(frontendUrl + "/payment/cancel")
            .setCustomerEmail(user.getEmail())
            .setClientReferenceId(user.getId().toString())
            .putMetadata("user_id", user.getId().toString())
            .putMetadata("package_id", creditPackage.getId().toString())
            .putMetadata("package_code", creditPackage.getCode())
            .addLineItem(
            		 SessionCreateParams.LineItem.builder()
                     .setQuantity(1L)
                     .setPriceData(
                         SessionCreateParams.LineItem.PriceData.builder()
                             .setCurrency("eur")
                             .setUnitAmount(creditPackage.getPrice().multiply(BigDecimal.valueOf(100)).longValue())
                             .setProductData(
                                     SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                         .setName(creditPackage.getName())
                                         .setDescription(creditPackage.getDescription())
                                         .putMetadata("package_id", creditPackage.getId().toString())
                                         .build()
                                 )
                                 .build()
                         )
                         .build()
                 )
                 .build();

             Session session = Session.create(params);
             return session.getUrl();
    }

    public ResponseEntity<String> handleWebhook(String payload, String sigHeader) {
        System.out.println("🔄 WEBHOOK RECEIVED!");
        
        try {
            Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
            System.out.println("✅ Signature verified! Event type: " + event.getType());
            
            if (!"checkout.session.completed".equals(event.getType())) {
                System.out.println("ℹ️ Ignoring event: " + event.getType());
                return ResponseEntity.ok("Event ignored");
            }
            
            System.out.println("💰 PAGAMENTO COMPLETATO RILEVATO!");
            
            // ✅ CORREZIONE: USA getObject() DIRETTAMENTE
            Session session = (Session) event.getData().getObject();
            
            // 5. Log dettagliato
            System.out.println("🔍 DETTAGLI SESSION:");
            System.out.println("   🆔 ID: " + session.getId());
            System.out.println("   📊 Status: " + session.getStatus());
            System.out.println("   💳 Payment Status: " + session.getPaymentStatus());
            System.out.println("   👤 User ID: " + session.getMetadata().get("user_id"));
            System.out.println("   📦 Package ID: " + session.getMetadata().get("package_id"));
            System.out.println("   📧 Email: " + session.getCustomerEmail());
            
            // 6. Verifica condizioni
            boolean isComplete = "complete".equals(session.getStatus());
            boolean isPaid = "paid".equals(session.getPaymentStatus());
            boolean hasMetadata = session.getMetadata().get("user_id") != null && 
                                 session.getMetadata().get("package_id") != null;
            
            System.out.println("🔍 CONDIZIONI:");
            System.out.println("   ✅ Session complete: " + isComplete);
            System.out.println("   ✅ Payment paid: " + isPaid);
            System.out.println("   ✅ Metadata present: " + hasMetadata);
            
            if (isComplete && isPaid && hasMetadata) {
                System.out.println("✅✅✅ TUTTE LE CONDIZIONI SODDISFATTE - AGGIUNGO CREDITI!");
                handleSuccessfulPayment(session);
            } else {
                System.out.println("❌ Condizioni non soddisfatte - skipping");
            }
            
            return ResponseEntity.ok("Webhook processed");
            
        } catch (Exception e) {
            System.err.println("❌ ERRORE CRITICO: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Webhook error");
        }
    }

    private void handleSuccessfulPayment(Session session) {
        try {
            String userId = session.getMetadata().get("user_id");
            String packageId = session.getMetadata().get("package_id");
            String packageCode = session.getMetadata().get("package_code");
            
            User user = userService.findById(Long.parseLong(userId))
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
            
            CreditPackage creditPackage = creditPackageService.findById(Long.parseLong(packageId))
                .orElseThrow(() -> new RuntimeException("Package not found: " + packageId));

            // ✅ CORREZIONE: Usa la firma corretta
            userService.addCredits(user, creditPackage.getCredits(), 
                "Acquisto pacchetto: " + creditPackage.getName(), 
                "stripe_" + session.getId());
            
            System.out.println("Payment successful - User: " + user.getEmail() + 
                             ", Package: " + creditPackage.getName() + 
                             ", Credits: " + creditPackage.getCredits());
            
        } catch (Exception e) {
            System.err.println("Error processing successful payment: " + e.getMessage());
        }
    }

  
}