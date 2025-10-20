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
        System.out.println("📦 Payload length: " + payload.length());
        System.out.println("🔐 Signature present: " + (sigHeader != null));
        System.out.println("🔑 Webhook secret configured: " + (webhookSecret != null && !webhookSecret.isEmpty()));
        
        try {
            System.out.println("🔍 Verifying signature...");
            Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
            System.out.println("✅ Signature verified! Event type: " + event.getType());
            
            if ("checkout.session.completed".equals(event.getType())) {
                System.out.println("💰 Checkout session completed - processing payment...");
                Session session = (Session) event.getDataObjectDeserializer().getObject().get();
                System.out.println("🎫 Session ID: " + session.getId());
                System.out.println("👤 User ID from metadata: " + session.getMetadata().get("user_id"));
                System.out.println("📦 Package ID from metadata: " + session.getMetadata().get("package_id"));
                
                handleSuccessfulPayment(session);
            } else {
                System.out.println("ℹ️ Ignoring event type: " + event.getType());
            }
            
            return ResponseEntity.ok("Webhook processed successfully");
            
        } catch (SignatureVerificationException e) {
            System.err.println("❌ SIGNATURE VERIFICATION FAILED: " + e.getMessage());
            System.err.println("🔑 Expected secret: " + webhookSecret);
            return ResponseEntity.badRequest().body("Invalid signature");
        } catch (Exception e) {
            System.err.println("❌ WEBHOOK PROCESSING ERROR: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Webhook error: " + e.getMessage());
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