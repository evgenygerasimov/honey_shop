package org.site.honey_shop.serviceIT;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.site.honey_shop.TestContainerConfig;
import org.site.honey_shop.entity.Role;
import org.site.honey_shop.entity.User;
import org.site.honey_shop.repository.UserRepository;
import org.site.honey_shop.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class UserServiceIT extends TestContainerConfig {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void clearDb() {
        userRepository.deleteAll();
    }

    @Test
    void testSaveUser_success() {
        User user = buildSampleUser();
        MockMultipartFile image = new MockMultipartFile("image", "user.jpg", "image/jpeg", "test image".getBytes(StandardCharsets.UTF_8));

        User saved = userService.save(user, image);

        assertThat(saved.getUserId()).isNotNull();
        assertThat(saved.getUsername()).isEqualTo("testuser");
        assertThat(saved.getPhoto()).contains("/assets/img/");
        assertThat(passwordEncoder.matches("password", saved.getPassword())).isTrue();
    }

    @Test
    void testFindUserById_success() {
        User saved = userService.save(buildSampleUser(), null);

        var userDto = userService.findById(saved.getUserId());

        assertThat(userDto).isNotNull();
        assertThat(userDto.username()).isEqualTo("testuser");
    }

    @Test
    void testFindUserByUsername_success() {
        userService.save(buildSampleUser(), null);

        var userDto = userService.findByUsername("testuser");

        assertThat(userDto).isNotNull();
        assertThat(userDto.username()).isEqualTo("testuser");
    }

//    @Test
//    void testFindAllUsers() {
//        userService.save(buildSampleUser(), null);
//        User another = buildSampleUser();
//        another.setUsername("user2");
//        userService.save(another, null);
//
//        List<?> users = userService.findAll();
//
//        assertThat(users).hasSize(2);
//    }

    @Test
    void testRemoveImageFromUserProfile_success() {
        User saved = userService.save(buildSampleUser(), null);
        saved.setPhoto("/assets/img/user.jpg");
        userRepository.save(saved);

        boolean result = userService.removeImageFromUserProfile(saved.getUserId());

        assertThat(result).isTrue();
        User updated = userRepository.findById(saved.getUserId()).orElseThrow();
        assertThat(updated.getPhoto()).isNull();
    }

    private User buildSampleUser() {
        return User.builder()
                .username("testuser")
                .password("password")
                .firstName("Иван")
                .lastName("Иванов")
                .middleName("Иванович")
                .email("ivanov@example.com")
                .phone("1234567890")
                .birthDate(LocalDate.of(1990, 1, 1))
                .enabled(true)
                .role(Role.ROLE_SUPER_ADMIN)
                .build();
    }
}
