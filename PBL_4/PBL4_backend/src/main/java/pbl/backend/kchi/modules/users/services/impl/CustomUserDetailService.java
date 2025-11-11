package pbl.backend.kchi.modules.users.services.impl;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import pbl.backend.kchi.modules.users.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import pbl.backend.kchi.modules.users.entities.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import java.util.Collection;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailService  implements UserDetailsService{

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {

        User user = userRepository.findById(Long.valueOf(userId)).orElseThrow(() -> new UsernameNotFoundException("Người dùng không tồn tại!"));
        return new org.springframework.security.core.userdetails.User(
          user.getEmail(),
          user.getPassword(),
                Collections.emptyList()

        );

    }
}