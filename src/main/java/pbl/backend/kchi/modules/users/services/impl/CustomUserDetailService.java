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

        System.out.println("---------- VẠCH TRẦN QUYỀN ----------");
        System.out.println("User ID: " + user.getId());
        System.out.println("Email: " + user.getEmail());
        System.out.println("Số lượng nhóm quyền (UserCatalogues): " + user.getUserCatalogues().size());

        if (user.getUserCatalogues().isEmpty()) {
            System.out.println("!!! CẢNH BÁO: User này chưa thuộc Nhóm quyền (Role) nào cả!");
        } else {
            user.getUserCatalogues().forEach(cat -> {
                System.out.println(" - Thuộc nhóm: " + cat.getName() + " (ID: " + cat.getId() + ")");
                System.out.println("   -> Số quyền trong nhóm này: " + cat.getPermissions().size());
            });
        }

        System.out.println("--- DANH SÁCH QUYỀN CUỐI CÙNG ---");
        authorities.forEach(auth -> System.out.println(" [x] " + auth.getAuthority()));
        System.out.println("-------------------------------------");

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