package org.site.honey_shop.service;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.site.honey_shop.dto.UserResponseDTO;
import org.site.honey_shop.entity.Authority;
import org.site.honey_shop.entity.Role;
import org.site.honey_shop.entity.User;
import org.site.honey_shop.exception.ImageUploadException;
import org.site.honey_shop.exception.MyAuthenticationException;
import org.site.honey_shop.mapper.ShopMapper;
import org.site.honey_shop.repository.UserRepository;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ShopMapper shopMapper;

    @Mock
    private AuthorityService authorityService;

    @Mock
    private Principal principal;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    private final UUID userId = UUID.randomUUID();
    private User user;
    private UserResponseDTO userDto;

    @BeforeEach
    void setUp() {
        userService.UPLOAD_DIRECTORY = "uploads";
        user = User.builder()
                .userId(userId)
                .username("testuser")
                .password("password")
                .role(Role.ROLE_SUPER_ADMIN)
                .enabled(true)
                .build();

        userDto = new UserResponseDTO(userId,
                "testuser",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                Role.ROLE_SUPER_ADMIN,
                true,
                null
        );
    }

    @Test
    void findById_shouldReturnUserDto() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(shopMapper.toUserDto(user)).thenReturn(userDto);

        UserResponseDTO result = userService.findById(userId);

        assertEquals(userId, result.userId());
    }

    @Test
    void findById_shouldThrowExceptionWhenNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> userService.findById(userId));
    }

    @Test
    void findByUsername_shouldReturnUserDto() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(shopMapper.toUserDto(user)).thenReturn(userDto);

        UserResponseDTO result = userService.findByUsername("testuser");

        assertEquals("testuser", result.username());
    }

    @Test
    void findAll_shouldReturnListOfDtos() {
        List<User> users = List.of(user);
        when(userRepository.findAll()).thenReturn(users);
        when(shopMapper.toUserDto(user)).thenReturn(userDto);

        List<UserResponseDTO> result = userService.findAll();

        assertEquals(1, result.size());
    }

    @Test
    void save_shouldSaveUserWithImage() {
        MockMultipartFile file = new MockMultipartFile("image", "image.jpg", "image/jpeg", "fake content".getBytes());
        User savedUser = user.toBuilder().photo("/assets/img/image.jpg").build();

        when(userRepository.save(any())).thenReturn(savedUser);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(authorityService.save(any())).thenReturn(null);

        user.setPassword("plain");
        User result = userService.save(user, file);

        assertEquals("/assets/img/image.jpg", result.getPhoto());
        verify(authorityService).save(any(Authority.class));
    }

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void update_shouldUpdateUserIfAllowed() {
        User existingUser = user.toBuilder().email("old@mail.com").build();
        User currentUser = user;

        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(currentUser));
        when(userRepository.findById(any())).thenReturn(Optional.of(existingUser));
        when(principal.getName()).thenReturn("testuser");
        when(userRepository.save(any())).thenReturn(existingUser);
        when(authorityService.findAuthorityByUser(existingUser)).thenReturn(new Authority());

        User updated = user.toBuilder().email("new@mail.com").build();
        User result = userService.update(updated, null, principal);

        assertEquals("new@mail.com", result.getEmail());
    }

    @Test
    void update_shouldThrowWhenUnauthorized() {
        User target = user.toBuilder()
                .userId(UUID.randomUUID())
                .build();

        User currentUser = user.toBuilder()
                .role(Role.ROLE_ADMIN)
                .build();

        when(principal.getName()).thenReturn("testuser");
        when(userRepository.findByUsername(eq("testuser"))).thenReturn(Optional.of(currentUser));
        when(userRepository.findById(any())).thenReturn(Optional.of(target));

        MyAuthenticationException exception = assertThrows(MyAuthenticationException.class,
                () -> userService.update(target, null, principal));

        assertNotNull(exception);
    }

    @Test
    void delete_shouldDeleteUser() {
        User other = user.toBuilder().userId(UUID.randomUUID()).build();
        when(userRepository.findById(other.getUserId())).thenReturn(Optional.of(other));
        doNothing().when(userRepository).delete(other);

        userService = spy(userService);
        doReturn("some-other-id").when(userService).getCurrentUserId();

        assertDoesNotThrow(() -> userService.delete(other.getUserId()));
    }

    @Test
    void delete_shouldThrowWhenDeletingSelf() {
        userService = spy(userService);
        doReturn(userId.toString()).when(userService).getCurrentUserId();

        assertThrows(MyAuthenticationException.class, () -> userService.delete(userId));
    }

    @Test
    void imageSelectionProcessing_shouldReturnUrlOnSuccess() {
        MockMultipartFile file = new MockMultipartFile("image", "image.jpg", "image/jpeg", "abc".getBytes());

        String url = userService.imageSelectionProcessing(file);

        assertEquals("/assets/img/image.jpg", url);
    }

    @Test
    void imageSelectionProcessing_shouldThrowOnFailure() throws IOException {
        MultipartFile badFile = mock(MultipartFile.class);
        when(badFile.isEmpty()).thenReturn(false);
        when(badFile.getOriginalFilename()).thenReturn("bad.jpg");
        when(badFile.getBytes()).thenThrow(IOException.class);

        assertThrows(ImageUploadException.class, () -> userService.imageSelectionProcessing(badFile));
    }

    @Test
    void removeImageFromUserProfile_shouldSetPhotoNull() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        assertDoesNotThrow(() -> userService.removeImageFromUserProfile(userId));
    }

    @Test
    void userDetailsService_shouldReturnUserDetails() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        UserDetails result = userService.userDetailsService().loadUserByUsername("testuser");

        assertEquals("testuser", result.getUsername());
    }

    @Test
    void userDetailsService_shouldThrowWhenUserNotFound() {
        when(userRepository.findByUsername("notfound")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> userService.userDetailsService().loadUserByUsername("notfound"));
    }
}
