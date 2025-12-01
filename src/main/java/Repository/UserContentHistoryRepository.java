package Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import model.Content;

@Repository
public interface UserContentHistoryRepository extends JpaRepository<Content, Long> {
    List<Content> findTop5ByBrandIdOrderByCreatedAtDesc(Long brandId);
}