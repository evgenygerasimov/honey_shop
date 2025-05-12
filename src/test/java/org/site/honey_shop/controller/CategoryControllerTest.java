package org.site.honey_shop.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.site.honey_shop.dto.UserResponseDTO;
import org.site.honey_shop.service.CategoryService;
import org.site.honey_shop.service.UserService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CategoryControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CategoryService categoryService;

    @Mock
    private UserService userService;

    @InjectMocks
    private CategoryController categoryController;

    private UserResponseDTO user;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders
                .standaloneSetup(categoryController)
                .build();

        var authentication = new UsernamePasswordAuthenticationToken("admin", null);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        user = new UserResponseDTO(
                UUID.randomUUID(),
                "user",
                "Ivan",
                "Ivanov",
                "Ivanovich",
                "email@example.com",
                "89012345678",
                null,
                null,
                null,
                null,
                null
        );
    }

    @Test
    void testShowCategoryForm() throws Exception {
        when(userService.findByUsername("admin"))
                .thenReturn(user);

        mockMvc.perform(get("/categories/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("add-category"))
                .andExpect(model().attributeExists("userId"))
                .andExpect(model().attributeExists("category"));
    }

    @Test
    void testCreateCategory_Valid() throws Exception {
        when(userService.findByUsername("admin"))
                .thenReturn(user);

        mockMvc.perform(post("/categories")
                        .param("name", "New Category"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/products"));

        verify(categoryService, times(1)).save("New Category");
    }

    @Test
    void testCreateCategory_WithValidationError() throws Exception {
        when(userService.findByUsername("admin"))
                .thenReturn(user);

        mockMvc.perform(post("/categories")
                        .param("name", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("add-category"))
                .andExpect(model().attributeExists("userId"));
    }

    @Test
    void testCreateCategory_DuplicateNameThrowsException() throws Exception {
        when(userService.findByUsername("admin"))
                .thenReturn(user);

        doThrow(new IllegalArgumentException("Category already exists"))
                .when(categoryService).save("Existing Category");

        mockMvc.perform(post("/categories")
                        .param("name", "Existing Category"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/categories/new"))
                .andExpect(flash().attribute("errorMessage", "Category already exists"));
    }
}
