package Config;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import DTO.UserDTO;
import Service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mapper.DtoMapper;
import model.User;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserService userService;

    public JwtFilter(JwtUtil jwtUtil, UserService userService) {
        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

      

        // Estrai token dalla header Authorization
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtUtil.validateToken(token)) {
                String subject = jwtUtil.getSubject(token);
                try {
                    Long userId = Long.parseLong(subject);
                    Optional<User> o = userService.findById(userId);
                    if (o.isPresent()) {
                        User user = o.get();
                        UserDTO dto = DtoMapper.toDTO(user);
                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(dto, null, Collections.emptyList());
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                } catch (NumberFormatException ignored) { }
            }
        }

        filterChain.doFilter(request, response);
    }
}
