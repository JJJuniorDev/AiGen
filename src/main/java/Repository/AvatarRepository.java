package Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import model.Avatar;

public interface AvatarRepository extends JpaRepository<Avatar, Long> {
    List<Avatar> findByCategory(String category);
    List<Avatar> findByNameContainingIgnoreCase(String name);
}