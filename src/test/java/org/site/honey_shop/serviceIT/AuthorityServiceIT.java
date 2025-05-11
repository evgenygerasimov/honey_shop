package org.site.honey_shop.serviceIT;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.site.honey_shop.TestContainerConfig;
import org.site.honey_shop.entity.Authority;
import org.site.honey_shop.entity.Role;
import org.site.honey_shop.entity.User;
import org.site.honey_shop.repository.AuthorityRepository;
import org.site.honey_shop.repository.UserRepository;
import org.site.honey_shop.service.AuthorityService;
import org.site.honey_shop.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AuthorityServiceIT extends TestContainerConfig {

    @Autowired
    private AuthorityService authorityService;

    @Autowired
    private UserService userService;

    @Autowired
    private AuthorityRepository authorityRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void clearDb() {
        authorityRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void testSaveAuthority() {
        User user = User.builder()
                .username("testuser")
                .password("Password1!")
                .firstName("Test")
                .lastName("User")
                .middleName("Middle")
                .email("test@example.com")
                .phone("+7 (123) 456-78-90")
                .birthDate(LocalDate.of(2000, 1, 1))
                .role(Role.ROLE_SUPER_ADMIN)
                .enabled(true)
                .build();

        user = userService.save(user, null);

        Authority authority = new Authority();
        authority.setAuthority(user.getRole().name());
        authority.setUser(user);
        authority.setUsername(user.getUsername());

        Authority savedAuthority = authorityService.save(authority);

        assertThat(savedAuthority.getId()).isNotNull();
        assertThat(savedAuthority.getAuthority()).isEqualTo("ROLE_SUPER_ADMIN");
        assertThat(savedAuthority.getUser().getUsername()).isEqualTo("testuser");
    }

    @Test
    void testFindAuthorityByUser() {

        User user = User.builder()
                .username("seconduser")
                .password("Password1!")
                .firstName("Second")
                .lastName("User")
                .middleName("Middle")
                .email("second@example.com")
                .phone("+7 (321) 654-98-76")
                .birthDate(LocalDate.of(1995, 5, 10))
                .role(Role.ROLE_ADMIN)
                .enabled(true)
                .build();

        user = userService.save(user, null);

        Authority foundAuthority = authorityService.findAuthorityByUser(user);

        assertThat(foundAuthority).isNotNull();
        assertThat(foundAuthority.getAuthority()).isEqualTo("ROLE_ADMIN");
    }
}
