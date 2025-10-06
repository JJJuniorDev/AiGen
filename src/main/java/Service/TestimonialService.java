package Service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import Repository.TestimonialRepository;
import model.Testimonial;
import model.User;

@Service
public class TestimonialService {

    @Autowired
    private TestimonialRepository testimonialRepository;

    public Testimonial save(Testimonial testimonial) {
        return testimonialRepository.save(testimonial);
    }

    public List<Testimonial> getByUser(User user) {
        return testimonialRepository.findByUser(user);
    }
}
