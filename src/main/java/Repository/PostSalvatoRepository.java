package Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import model.PostSalvato;

import java.util.List;

@Repository
public interface PostSalvatoRepository extends JpaRepository<PostSalvato, Long> {
 
 List<PostSalvato> findByUserIdOrderByDataSalvataggioDesc(Long userId);
 
 List<PostSalvato> findByUserIdAndTipoOrderByDataSalvataggioDesc(Long userId, String tipo);
 
 List<PostSalvato> findByUserIdAndPiattaformaOrderByDataSalvataggioDesc(Long userId, String piattaforma);
 
 List<PostSalvato> findByUserIdAndTipoAndPiattaformaOrderByDataSalvataggioDesc(Long userId, String tipo, String piattaforma);
 
 @Query("SELECT DISTINCT p.tipo FROM PostSalvato p WHERE p.user.id = :userId")
 List<String> findDistinctTipiByUserId(Long userId);
 
 @Query("SELECT DISTINCT p.piattaforma FROM PostSalvato p WHERE p.user.id = :userId")
 List<String> findDistinctPiattaformeByUserId(Long userId);
 
 void deleteByIdAndUserId(Long id, Long userId);
}