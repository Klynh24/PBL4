package pbl.backend.kchi.modules.users.services.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pbl.backend.kchi.modules.users.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import pbl.backend.kchi.modules.users.entities.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.GrantedAuthority; // Import cần thiết
import org.springframework.security.core.authority.SimpleGrantedAuthority; // Import cần thiết
import pbl.backend.kchi.modules.users.resources.CustomUserDetail;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class CustomUserDetailService implements  UserDetailsService {

    private final UserRepository userRepository;

    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailService.class);

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException{

        User user = userRepository.findById(Long.valueOf(userId)).orElseThrow(() -> new UsernameNotFoundException("User không tồn tại"));



        List<GrantedAuthority> authorities = user.getUserCatalogues().stream()
                .flatMap(catalogue -> catalogue.getPermissions().stream())
                .map(permission -> new SimpleGrantedAuthority(permission.getName()))
                .collect(Collectors.toList());


        logger.info("authorities: {}", authorities.size());

        return new CustomUserDetail(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                authorities
        );

        // return new org.springframework.security.core.userdetails.User(
        //     user.getEmail(),
        //     user.getPassword(),
        //     authorities
        // );
    }


}