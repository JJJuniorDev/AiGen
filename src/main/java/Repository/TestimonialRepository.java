package Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import model.Testimonial;
import model.User;

public interface TestimonialRepository extends JpaRepository<Testimonial, Long> {
    List<Testimonial> findByUser(User user);
}