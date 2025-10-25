package Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import DTO.PostSalvatoDTO;
import Service.PostSalvatoService;
import model.PostSalvato;
import model.User;
import DTO.UserDTO;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/posts-salvati")
@CrossOrigin(origins = "*")
public class PostSalvatoController {
 
 @Autowired
 private PostSalvatoService postSalvatoService;
 
 @PostMapping
 public ResponseEntity<?> salvaPost(@RequestBody PostSalvatoDTO postDTO, 
                                  @AuthenticationPrincipal UserDTO userDTO) {
     try {
         PostSalvato post = convertToEntity(postDTO);
         User user = new User();
         user.setId(Long.valueOf(userDTO.getId())); // Prendi l'ID dall'utente autenticato
         post.setUser(user);
         PostSalvato salvato = postSalvatoService.salvaPost(post);
         return ResponseEntity.ok(convertToDTO(salvato));
     } catch (Exception e) {
         return ResponseEntity.badRequest().body("Errore nel salvataggio: " + e.getMessage());
     }
 }
 
 @GetMapping
 public ResponseEntity<List<PostSalvatoDTO>> getPostSalvati(@AuthenticationPrincipal UserDTO userDTO) {
     // 👆 Cambia da User a UserDTO
     
     System.out.println("🔍 DEBUG Controller - UserDTO: " + userDTO);
     
     if (userDTO == null) {
         System.out.println("🚨 ERRORE: UserDTO è NULL nel controller!");
         return ResponseEntity.status(403).build();
     }
     
     // Converti UserDTO in User se necessario, oppure usa direttamente l'ID
     List<PostSalvato> posts = postSalvatoService.getPostSalvatiByUser(Long.valueOf(userDTO.getId()));
     List<PostSalvatoDTO> dtos = posts.stream()
         .map(this::convertToDTO)
         .collect(Collectors.toList());
     return ResponseEntity.ok(dtos);
 }
 @GetMapping("/tipo/{tipo}")
 public ResponseEntity<List<PostSalvatoDTO>> getPostSalvatiByTipo(@PathVariable String tipo,
                                                                 @AuthenticationPrincipal User user) {
     List<PostSalvato> posts = postSalvatoService.getPostSalvatiByTipo(user.getId(), tipo);
     List<PostSalvatoDTO> dtos = posts.stream()
         .map(this::convertToDTO)
         .collect(Collectors.toList());
     return ResponseEntity.ok(dtos);
 }
 
 @GetMapping("/piattaforma/{piattaforma}")
 public ResponseEntity<List<PostSalvatoDTO>> getPostSalvatiByPiattaforma(@PathVariable String piattaforma,
                                                                       @AuthenticationPrincipal User user) {
     List<PostSalvato> posts = postSalvatoService.getPostSalvatiByPiattaforma(user.getId(), piattaforma);
     List<PostSalvatoDTO> dtos = posts.stream()
         .map(this::convertToDTO)
         .collect(Collectors.toList());
     return ResponseEntity.ok(dtos);
 }
 
 @DeleteMapping("/{id}")
 public ResponseEntity<?> eliminaPostSalvato(@PathVariable Long id,
                                           @AuthenticationPrincipal User user) {
     boolean eliminato = postSalvatoService.eliminaPostSalvato(id, user.getId());
     if (eliminato) {
         return ResponseEntity.ok().build();
     } else {
         return ResponseEntity.notFound().build();
     }
 }
 
 @GetMapping("/tipi")
 public ResponseEntity<List<String>> getTipiDisponibili(@AuthenticationPrincipal User user) {
     List<String> tipi = postSalvatoService.getTipiDisponibili(user.getId());
     return ResponseEntity.ok(tipi);
 }
 
 @GetMapping("/piattaforme")
 public ResponseEntity<List<String>> getPiattaformeDisponibili(@AuthenticationPrincipal User user) {
     List<String> piattaforme = postSalvatoService.getPiattaformeDisponibili(user.getId());
     return ResponseEntity.ok(piattaforme);
 }
 
 private PostSalvato convertToEntity(PostSalvatoDTO dto) {
     PostSalvato post = new PostSalvato();
     post.setContenuto(dto.getContenuto());
     post.setTipo(dto.getTipo());
     post.setPiattaforma(dto.getPiattaforma());
     post.setCategoria(dto.getCategoria());
     post.setBrandId(dto.getBrandId());
     post.setBrandName(dto.getBrandName());
     return post;
 }
 
 private PostSalvatoDTO convertToDTO(PostSalvato post) {
     return new PostSalvatoDTO(
         post.getId(),
         post.getContenuto(),
         post.getTipo(),
         post.getPiattaforma(),
         post.getCategoria(),
         post.getBrandId(),
         post.getBrandName(),
         post.getDataSalvataggio()
     );
 }
}