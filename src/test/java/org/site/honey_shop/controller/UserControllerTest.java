package org.site.honey_shop.controller;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.site.honey_shop.dto.UserResponseDTO;
import org.site.honey_shop.entity.Role;
import org.site.honey_shop.entity.User;
import org.site.honey_shop.exception.ImageUploadException;
import org.site.honey_shop.exception.MyAuthenticationException;
import org.site.honey_shop.repository.UserRepository;
import org.site.honey_shop.service.UserService;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.security.Principal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserControllerTest {

    @InjectMocks
    private UserController userController;
    @Mock
    private UserService userService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private Principal principal;

    private MockMultipartFile image;

    private MockMvc mockMvc;

    private UserResponseDTO userResponseDTO;

    private User user;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setValidator(new LocalValidatorFactoryBean() {
                    @Override
                    public boolean supports(Class<?> clazz) {
                        return false;
                    }
                })
                .build();

        var authentication = new UsernamePasswordAuthenticationToken("admin", null);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        var userDto = mock(UserResponseDTO.class);
        when(userDto.userId()).thenReturn(UUID.randomUUID());
        when(userService.findByUsername("admin")).thenReturn(userDto);

        image = new MockMultipartFile(
                "image",
                "image.jpg",
                "image/jpeg",
                "image-data".getBytes()
        );
        userResponseDTO = new UserResponseDTO(
                UUID.randomUUID(),
                "user",
                "Ivan",
                "Ivanov",
                "Ivanovich",
                "email@example.com",
                "+7 (123) 456-78-90",
                null,
                null,
                null,
                null,
                null
        );

        user = User.builder()
                .userId(UUID.randomUUID())
                .username("user")
                .password("SecurePass1@")
                .firstName("Ivan")
                .lastName("Ivanov")
                .middleName("Ivanovich")
                .email("email@example.com")
                .phone("+7 (123) 456-78-90")
                .photo("profile_pic.jpg")
                .birthDate(LocalDate.of(1990, 5, 15))
                .role(Role.ROLE_SUPER_ADMIN)  // Это пример роли, возможно будет использоваться другая роль
                .enabled(true)
                .build();

    }

    @Test
    void testShowUserList() throws Exception {
        when(userService.findAll()).thenReturn(Collections.singletonList(userResponseDTO));

        mockMvc.perform(get("/users/list"))
                .andExpect(status().isOk())
                .andExpect(view().name("all-users"))
                .andExpect(model().attributeExists("users"));
    }

    @Test
    void testShowCreateUserForm() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("add-user"))
                .andExpect(model().attributeExists("user"));
    }


    @Test
    void testCreateUser_withImageUploadException() throws Exception {
        when(userService.save(any(), any())).thenThrow(new ImageUploadException("Ошибка загрузки изображения!"));

        mockMvc.perform(multipart("/users")
                        .file(image)
                        .flashAttr("user", user))
                .andExpect(status().is3xxRedirection())  // Ожидаем редирект
                .andExpect(view().name("redirect:/users"))  // Ожидаем редирект на /users
                .andExpect(flash().attributeExists("errorMessage"))
                .andExpect(flash().attributeExists("user"));  // Проверяем, что атрибут user добавлен (опционально)
    }

    @Test
    void testShowUserData() throws Exception {
        UUID userId = UUID.randomUUID();

        when(userService.findById(userId)).thenReturn(userResponseDTO);
        when(userService.findByUsername(anyString())).thenReturn(userResponseDTO);

        mockMvc.perform(get("/users/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(view().name("user-data"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("authUserId"));
    }

    @Test
    void testShowUserEditForm() throws Exception {
        when(principal.getName()).thenReturn("admin");
        UUID userId = UUID.randomUUID();
        when(userService.findByUsername(anyString())).thenReturn(userResponseDTO);
        when(userService.findById(userId)).thenReturn(userResponseDTO);

        mockMvc.perform(get("/users/edit_form/{userId}", userId)
                        .principal(principal))
                .andExpect(status().isOk())
                .andExpect(view().name("edit-user"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("authUserId"));
    }

    @Test
    void testUpdateUser_withValidationErrors() throws Exception {
        when(principal.getName()).thenReturn("admin");
        when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));

        mockMvc.perform(multipart("/users/edit")
                        .file(image)
                        .flashAttr("user", user)
                        .param("userId", user.getUserId().toString())
                        .principal(principal))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users/" + user.getUserId().toString()));
    }

    @Test
    void testDeleteUser() throws Exception {
        UUID userId = UUID.randomUUID();
        doNothing().when(userService).delete(userId);

        mockMvc.perform(post("/users/delete/{userId}", userId))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/users/list"));
    }

    @Test
    void testDeleteUser_withException() throws Exception {
        UUID userId = UUID.randomUUID();
        doThrow(new MyAuthenticationException("You cannot delete yourself")).when(userService).delete(userId);

        mockMvc.perform(post("/users/delete/{userId}", userId))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/users/list"))
                .andExpect(flash().attribute("accessDeniedMessage", "Вы не можете удалить сами себя."));
    }

    @Test
    void testDeleteImage() throws Exception {
        String imageFilename = "image.jpg";
        String userId = UUID.randomUUID().toString();
        when(userService.removeImageFromUserProfile(any(UUID.class))).thenReturn(true);

        mockMvc.perform(post("/users/delete-image")
                        .param("imageFilename", imageFilename)
                        .param("userId", userId))
                .andExpect(status().isOk())
                .andExpect(content().string("Photo from user profile was deleted successfully."));
    }
}
