package Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;
    
    @Value("${app.environment:development}")
    private String environment;
    
    public void sendVerificationEmail(String toEmail, String token, String userEmail) {
       
            String verificationUrl = baseUrl + "/api/auth/verify-email?token=" + token;
            
            // In sviluppo, logga il link invece di inviare email
            if ("development".equals(environment)) {
                logVerificationLink(toEmail, verificationUrl);
                return;
            }
            // In produzione, invia email reale
            try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Conferma il tuo indirizzo email");
            message.setText(createEmailText(userEmail, verificationUrl));
                
            mailSender.send(message);
        } catch (Exception e) {
        	System.err.println("❌ Errore nell'invio email a " + toEmail + ": " + e.getMessage());
            throw new RuntimeException("Errore nell'invio dell'email di verifica", e);
        }
    }
    
    private String createEmailText(String userEmail, String verificationUrl) {
        return "Ciao " + userEmail + ",\n\n" +
               "Benvenuto in SocialCraft! Per iniziare a utilizzare il tuo account, " +
               "conferma il tuo indirizzo email cliccando sul link seguente:\n\n" +
               verificationUrl + "\n\n" +
               "Questo link scadrà tra 24 ore.\n\n" +
               "Dopo la verifica riceverai 5 crediti gratuiti per iniziare!\n\n" +
               "Grazie,\nIl Team SocialCraft";
    }
    
    private void logVerificationLink(String toEmail, String verificationUrl) {
        System.out.println("=".repeat(80));
        System.out.println("📧 EMAIL DI VERIFICA (SIMULATA - DEVELOPMENT)");
        System.out.println("A: " + toEmail);
        System.out.println("🔗 LINK DI VERIFICA: " + verificationUrl);
        System.out.println("=".repeat(80));
    }
}