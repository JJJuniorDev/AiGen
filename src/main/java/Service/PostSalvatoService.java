package Service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import Repository.PostSalvatoRepository;
import model.PostSalvato;

import java.util.List;
import java.util.Optional;

@Service
public class PostSalvatoService {
 
 @Autowired
 private PostSalvatoRepository postSalvatoRepository;
 
 public PostSalvato salvaPost(PostSalvato post) {
     return postSalvatoRepository.save(post);
 }
 
 public List<PostSalvato> getPostSalvatiByUser(Long userId) {
     return postSalvatoRepository.findByUserIdOrderByDataSalvataggioDesc(userId);
 }
 
 public List<PostSalvato> getPostSalvatiByTipo(Long userId, String tipo) {
     return postSalvatoRepository.findByUserIdAndTipoOrderByDataSalvataggioDesc(userId, tipo);
 }
 
 public List<PostSalvato> getPostSalvatiByPiattaforma(Long userId, String piattaforma) {
     return postSalvatoRepository.findByUserIdAndPiattaformaOrderByDataSalvataggioDesc(userId, piattaforma);
 }
 
 public boolean eliminaPostSalvato(Long postId, Long userId) {
     Optional<PostSalvato> post = postSalvatoRepository.findById(postId);
     if (post.isPresent() && post.get().getUser().getId().equals(userId)) {
         postSalvatoRepository.deleteById(postId);
         return true;
     }
     return false;
 }
 
 public List<String> getTipiDisponibili(Long userId) {
     return postSalvatoRepository.findDistinctTipiByUserId(userId);
 }
 
 public List<String> getPiattaformeDisponibili(Long userId) {
     return postSalvatoRepository.findDistinctPiattaformeByUserId(userId);
 }
}