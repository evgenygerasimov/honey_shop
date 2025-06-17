package org.site.honey_shop.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.site.honey_shop.dto.UserResponseDTO;
import org.site.honey_shop.entity.Authority;
import org.site.honey_shop.entity.Role;
import org.site.honey_shop.entity.User;
import org.site.honey_shop.exception.ImageUploadException;
import org.site.honey_shop.exception.MyAuthenticationException;
import org.site.honey_shop.mapper.ShopMapper;
import org.site.honey_shop.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    @Value("${myapp.upload.image.directory}")
    public String UPLOAD_DIRECTORY;
    private final UserRepository userRepository;
    private final ShopMapper shopMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthorityService authorityService;

    public UserResponseDTO findById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
        log.info("User found with id: {}", userId);
        return shopMapper.toUserDto(user);
    }

    public UserResponseDTO findByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found with username: " + username));
        log.info("User found with username: {}", username);
        return shopMapper.toUserDto(user);
    }

    public Page<UserResponseDTO> findAll(Pageable pageable) {
        log.info("Find all users...");
        return userRepository.findAll(pageable).map(shopMapper::toUserDto);
    }

    public User save(User user, MultipartFile image) {
        String imageUrl = imageSelectionProcessing(image);
        user = User.builder()
                .username(user.getUsername())
                .password(passwordEncoder.encode(user.getPassword()))
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .middleName(user.getMiddleName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .birthDate(user.getBirthDate())
                .role(user.getRole())
                .photo(imageUrl)
                .enabled(user.getEnabled())
                .build();
        log.info("Save user: {}", user.getUsername());
        user = userRepository.save(user);

        Authority authority = new Authority();
        authority.setUser(user);
        authority.setUsername(user.getUsername());
        authority.setAuthority(user.getRole().name());

        log.info("Save authority: {} for user: {}", authority.getAuthority(), user.getUsername());
        authorityService.save(authority);
        return user;
    }

    public User update(User user, MultipartFile image, Principal principal) {
        User currentUser = userRepository.findByUsername(principal.getName()).orElseThrow(()
                -> new EntityNotFoundException("User not found with username: " + principal.getName()));
        User existingUser = userRepository.findById(user.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        String imageUrl = imageSelectionProcessing(image);

        existingUser.setUsername(existingUser.getUsername());
        existingUser.setPassword(existingUser.getPassword());
        existingUser.setFirstName(user.getFirstName());
        existingUser.setLastName(user.getLastName());
        existingUser.setMiddleName(user.getMiddleName());
        existingUser.setEmail(user.getEmail());
        existingUser.setPhone(user.getPhone());
        if (!imageUrl.isEmpty()) {
            existingUser.setPhoto(imageUrl);
        }
        existingUser.setBirthDate(user.getBirthDate());

        boolean isSelfEdit = currentUser.getUserId().equals(existingUser.getUserId());
        boolean isSuperAdmin = currentUser.getRole() == Role.ROLE_SUPER_ADMIN;
        if (!isSuperAdmin && !isSelfEdit) {
            log.info("Attempting to edit user: {} without super admin privileges", user.getUsername());
            throw new MyAuthenticationException("Вы можете редактировать только свой профиль!");
        }
        if (!isSuperAdmin || isSelfEdit) {
            existingUser.setRole(currentUser.getRole());
            existingUser.setEnabled(currentUser.getEnabled());
        } else {
            existingUser.setRole(user.getRole());
            existingUser.setEnabled(user.getEnabled());
        }
        log.info("Update user: {}", existingUser.getUsername());
        existingUser = userRepository.save(existingUser);
        Authority existingAuthority = authorityService.findAuthorityByUser(existingUser);
        existingAuthority.setAuthority(existingUser.getRole().name());
        existingAuthority.setUsername(existingUser.getUsername());
        log.info("Update authority: {}", existingAuthority.getAuthority());
        authorityService.save(existingAuthority);

        return existingUser;
    }

    public void delete(UUID userId) {
        if (userId.toString().equals(getCurrentUserId())){
            log.info("Attempting to deleteCategory self profile by user: {}", userId);
            throw new MyAuthenticationException("Вы не можете удалить свой профиль!");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
        log.info("Delete user: {}", user.getUsername());
        userRepository.delete(user);
    }

    public String imageSelectionProcessing(MultipartFile image) {
        String imageUrl = "";
        if (image != null && !image.isEmpty()) {
            String fileName = image.getOriginalFilename();
            if (fileName != null && !fileName.trim().isEmpty()) {
                Path fileNameAndPath = Paths.get(UPLOAD_DIRECTORY, fileName);
                imageUrl = "/assets/img/" + fileName;
                try {
                    Files.createDirectories(fileNameAndPath.getParent());
                    Files.write(fileNameAndPath, image.getBytes());
                } catch (IOException e) {
                    log.error("Error while uploading image file for user profile.", e);
                    throw new ImageUploadException("Ошибка загрузки изображения!");
                }
            }
        }
        return imageUrl;
    }

    public boolean removeImageFromUserProfile(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found!"));
        user.setPhoto(null);
        log.info("Remove image from user profile: {}", user.getUsername());
        userRepository.save(user);
        return true;
    }

    public String getCurrentUserId() {
        return findByUsername(SecurityContextHolder.getContext().getAuthentication().getName())
                .userId()
                .toString();
    }

    public UserDetailsService userDetailsService() {
        return new UserDetailsService() {
            @Override
            public UserDetails loadUserByUsername(String username) {
                return userRepository.findByUsername(username)
                        .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
            }
        };
    }
}
