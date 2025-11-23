package Controller;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import Config.JwtUtil;
import DTO.AuthRequest;
import DTO.AuthResponse;
import DTO.UserDTO;
import Service.UserService;
import mapper.DtoMapper;
import model.User;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    public AuthController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }
    
  

    @PostMapping("/signup")
    public ResponseEntity<UserDTO> signup(@RequestBody AuthRequest req) {
        try {
            User u = userService.register(req.getEmail(), req.getPassword());
            return ResponseEntity.ok(DtoMapper.toDTO(u));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }
    


    
    @PostMapping("/resend-verification")
    public ResponseEntity<String> resendVerification(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        boolean sent = userService.resendVerificationEmail(email);
        
        if (sent) {
            return ResponseEntity.ok("Email di verifica inviata con successo.");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Impossibile inviare l'email di verifica. Verifica l'email inserita.");
        }
    }


    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest req) {
        Optional<User> o = userService.findByEmail(req.getEmail());
        if (o.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        User user = o.get();
        System.out.println("✅ User found:");
        System.out.println("   ID: " + user.getId());
        System.out.println("   Email: " + user.getEmail());
        System.out.println("   Credits: " + user.getCredits());
        System.out.println("   Credits class: " + (user.getCredits() != null ? user.getCredits().getClass() : "null"));
        System.out.println("   Plan: " + user.getPlan());
        System.out.println("   MaxBrands: " + user.getMaxBrands());
        System.out.println("   Active: " + user.getActive());
        // Verifica se è un proxy
        if (org.hibernate.Hibernate.isInitialized(user)) {
            System.out.println("   ✅ User is initialized");
        } else {
            System.out.println("   ❌ User is NOT initialized (proxy)");
        }
        
        if (!userService.matchesPassword(user, req.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String token = jwtUtil.generateToken(user.getId().toString());
        UserDTO dto = DtoMapper.toDTO(user);
        // Debug del DTO
        System.out.println("=== DTO MAPPING ===");
        System.out.println("   DTO Credits: " + dto.getCredits());
        System.out.println("   DTO ID: " + dto.getId());
        System.out.println("   DTO Email: " + dto.getEmail());
        return ResponseEntity.ok(new AuthResponse(token, dto));
    }
    
    
    @GetMapping("/verify-email")
    public ResponseEntity<String> verifyEmail(@RequestParam String token) {
        boolean verified = userService.verifyEmail(token);
        String htmlResponse;
        
        if (verified) {
            htmlResponse = """
                <!DOCTYPE html>
                <html lang="it">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Email Verificata - AI SocialCraft Generator</title>
                    <meta http-equiv="refresh" content="3;url=https://ai-gen-fe.vercel.app">
                    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet">
                     <script>
                   
                    localStorage.setItem('emailJustVerified', 'true');
                    localStorage.setItem('pendingEmailLogin', 'true');
                    
                    window.location.href = 'https://ai-gen-fe.vercel.app';
                </script>
                    <style>
                        * {
                            margin: 0;
                            padding: 0;
                            box-sizing: border-box;
                        }
                        
                        body {
                            font-family: 'Inter', sans-serif;
                            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                            min-height: 100vh;
                            display: flex;
                            align-items: center;
                            justify-content: center;
                            padding: 20px;
                        }
                        
                        .container {
                            background: rgba(255, 255, 255, 0.95);
                            backdrop-filter: blur(20px);
                            border-radius: 24px;
                            padding: 60px 40px;
                            text-align: center;
                            box-shadow: 0 20px 60px rgba(0, 0, 0, 0.1);
                            border: 1px solid rgba(255, 255, 255, 0.2);
                            max-width: 500px;
                            width: 100%;
                            animation: fadeInUp 0.6s ease-out;
                        }
                        
                        .success-icon {
                            width: 80px;
                            height: 80px;
                            background: linear-gradient(135deg, #10b981, #059669);
                            border-radius: 50%;
                            display: flex;
                            align-items: center;
                            justify-content: center;
                            margin: 0 auto 30px;
                            animation: bounceIn 0.6s ease-out;
                        }
                        
                        .success-icon svg {
                            width: 40px;
                            height: 40px;
                            color: white;
                        }
                        
                        h1 {
                            color: #1f2937;
                            font-size: 2.5rem;
                            font-weight: 700;
                            margin-bottom: 16px;
                            background: linear-gradient(135deg, #1f2937, #374151);
                            -webkit-background-clip: text;
                            -webkit-text-fill-color: transparent;
                            background-clip: text;
                        }
                        
                        .subtitle {
                            color: #6b7280;
                            font-size: 1.1rem;
                            line-height: 1.6;
                            margin-bottom: 40px;
                            font-weight: 400;
                        }
                        
                        .redirect-text {
                            color: #9ca3af;
                            font-size: 0.9rem;
                            margin-bottom: 30px;
                            animation: pulse 2s infinite;
                        }
                        
                        .btn {
                            display: inline-flex;
                            align-items: center;
                            gap: 8px;
                            background: linear-gradient(135deg, #667eea, #764ba2);
                            color: white;
                            text-decoration: none;
                            padding: 16px 32px;
                            border-radius: 12px;
                            font-weight: 600;
                            font-size: 1rem;
                            transition: all 0.3s ease;
                            box-shadow: 0 4px 15px rgba(102, 126, 234, 0.3);
                        }
                        
                        .btn:hover {
                            transform: translateY(-2px);
                            box-shadow: 0 8px 25px rgba(102, 126, 234, 0.4);
                        }
                        
                        .loading-bar {
                            width: 100%;
                            height: 4px;
                            background: #e5e7eb;
                            border-radius: 2px;
                            margin-top: 30px;
                            overflow: hidden;
                        }
                        
                        .loading-progress {
                            height: 100%;
                            background: linear-gradient(90deg, #10b981, #059669);
                            border-radius: 2px;
                            animation: loading 3s linear;
                            animation-fill-mode: forwards;
                        }
                        
                        @keyframes fadeInUp {
                            from {
                                opacity: 0;
                                transform: translateY(30px);
                            }
                            to {
                                opacity: 1;
                                transform: translateY(0);
                            }
                        }
                        
                        @keyframes bounceIn {
                            0% {
                                opacity: 0;
                                transform: scale(0.3);
                            }
                            50% {
                                opacity: 1;
                                transform: scale(1.05);
                            }
                            70% {
                                transform: scale(0.9);
                            }
                            100% {
                                opacity: 1;
                                transform: scale(1);
                            }
                        }
                        
                        @keyframes pulse {
                            0%, 100% {
                                opacity: 1;
                            }
                            50% {
                                opacity: 0.7;
                            }
                        }
                        
                        @keyframes loading {
                            0% {
                                width: 0%;
                            }
                            100% {
                                width: 100%;
                            }
                        }
                        
                        @media (max-width: 480px) {
                            .container {
                                padding: 40px 24px;
                                margin: 20px;
                            }
                            
                            h1 {
                                font-size: 2rem;
                            }
                            
                            .success-icon {
                                width: 60px;
                                height: 60px;
                            }
                            
                            .success-icon svg {
                                width: 30px;
                                height: 30px;
                            }
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="success-icon">
                            <svg fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7"></path>
                            </svg>
                        </div>
                        
                        <h1>Email Verificata!</h1>
                        <p class="subtitle">La tua email è stata verificata con successo. Ora puoi accedere a tutte le funzionalità di AI Social Generator.</p>
                        
                        <p class="redirect-text">Reindirizzamento automatico in corso...</p>
                        
                        <a href="https://ai-gen-fe.vercel.app" class="btn">
                            <svg width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" d="M13 7l5 5m0 0l-5 5m5-5H6"></path>
                            </svg>
                            Vai all'Applicazione
                        </a>
                        
                        <div class="loading-bar">
                            <div class="loading-progress"></div>
                        </div>
                    </div>
                </body>
                </html>
                """;
        } else {
            htmlResponse = """
                <!DOCTYPE html>
                <html lang="it">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Errore Verifica - AI Social Generator</title>
                    <meta http-equiv="refresh" content="5;url=https://ai-gen-fe.vercel.app/login">
                    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet">
                    <style>
                        * {
                            margin: 0;
                            padding: 0;
                            box-sizing: border-box;
                        }
                        
                        body {
                            font-family: 'Inter', sans-serif;
                            background: linear-gradient(135deg, #f87171 0%, #dc2626 100%);
                            min-height: 100vh;
                            display: flex;
                            align-items: center;
                            justify-content: center;
                            padding: 20px;
                        }
                        
                        .container {
                            background: rgba(255, 255, 255, 0.95);
                            backdrop-filter: blur(20px);
                            border-radius: 24px;
                            padding: 60px 40px;
                            text-align: center;
                            box-shadow: 0 20px 60px rgba(0, 0, 0, 0.1);
                            border: 1px solid rgba(255, 255, 255, 0.2);
                            max-width: 500px;
                            width: 100%;
                            animation: fadeInUp 0.6s ease-out;
                        }
                        
                        .error-icon {
                            width: 80px;
                            height: 80px;
                            background: linear-gradient(135deg, #ef4444, #dc2626);
                            border-radius: 50%;
                            display: flex;
                            align-items: center;
                            justify-content: center;
                            margin: 0 auto 30px;
                            animation: shake 0.6s ease-out;
                        }
                        
                        .error-icon svg {
                            width: 40px;
                            height: 40px;
                            color: white;
                        }
                        
                        h1 {
                            color: #1f2937;
                            font-size: 2.5rem;
                            font-weight: 700;
                            margin-bottom: 16px;
                            background: linear-gradient(135deg, #dc2626, #ef4444);
                            -webkit-background-clip: text;
                            -webkit-text-fill-color: transparent;
                            background-clip: text;
                        }
                        
                        .subtitle {
                            color: #6b7280;
                            font-size: 1.1rem;
                            line-height: 1.6;
                            margin-bottom: 40px;
                            font-weight: 400;
                        }
                        
                        .redirect-text {
                            color: #9ca3af;
                            font-size: 0.9rem;
                            margin-bottom: 30px;
                            animation: pulse 2s infinite;
                        }
                        
                        .btn {
                            display: inline-flex;
                            align-items: center;
                            gap: 8px;
                            background: linear-gradient(135deg, #ef4444, #dc2626);
                            color: white;
                            text-decoration: none;
                            padding: 16px 32px;
                            border-radius: 12px;
                            font-weight: 600;
                            font-size: 1rem;
                            transition: all 0.3s ease;
                            box-shadow: 0 4px 15px rgba(239, 68, 68, 0.3);
                        }
                        
                        .btn:hover {
                            transform: translateY(-2px);
                            box-shadow: 0 8px 25px rgba(239, 68, 68, 0.4);
                        }
                        
                        .loading-bar {
                            width: 100%;
                            height: 4px;
                            background: #e5e7eb;
                            border-radius: 2px;
                            margin-top: 30px;
                            overflow: hidden;
                        }
                        
                        .loading-progress {
                            height: 100%;
                            background: linear-gradient(90deg, #ef4444, #dc2626);
                            border-radius: 2px;
                            animation: loading 5s linear;
                            animation-fill-mode: forwards;
                        }
                        
                        @keyframes fadeInUp {
                            from {
                                opacity: 0;
                                transform: translateY(30px);
                            }
                            to {
                                opacity: 1;
                                transform: translateY(0);
                            }
                        }
                        
                        @keyframes shake {
                            0%, 100% {
                                transform: translateX(0);
                            }
                            10%, 30%, 50%, 70%, 90% {
                                transform: translateX(-5px);
                            }
                            20%, 40%, 60%, 80% {
                                transform: translateX(5px);
                            }
                        }
                        
                        @keyframes pulse {
                            0%, 100% {
                                opacity: 1;
                            }
                            50% {
                                opacity: 0.7;
                            }
                        }
                        
                        @keyframes loading {
                            0% {
                                width: 0%;
                            }
                            100% {
                                width: 100%;
                            }
                        }
                        
                        @media (max-width: 480px) {
                            .container {
                                padding: 40px 24px;
                                margin: 20px;
                            }
                            
                            h1 {
                                font-size: 2rem;
                            }
                            
                            .error-icon {
                                width: 60px;
                                height: 60px;
                            }
                            
                            .error-icon svg {
                                width: 30px;
                                height: 30px;
                            }
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="error-icon">
                            <svg fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12"></path>
                            </svg>
                        </div>
                        
                        <h1>Errore di Verifica</h1>
                        <p class="subtitle">Il link di verifica non è valido o è scaduto. Ti stiamo reindirizzando alla pagina di login.</p>
                        
                        <p class="redirect-text">Reindirizzamento automatico in corso...</p>
                        
                        <a href="https://ai-gen-fe.vercel.app/login" class="btn">
                            <svg width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" d="M11 16l-4-4m0 0l4-4m-4 4h14m-5 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h7a3 3 0 013 3v1"></path>
                            </svg>
                            Vai al Login
                        </a>
                        
                        <div class="loading-bar">
                            <div class="loading-progress"></div>
                        </div>
                    </div>
                </body>
                </html>
                """;
        }
        
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(htmlResponse);
    }
}