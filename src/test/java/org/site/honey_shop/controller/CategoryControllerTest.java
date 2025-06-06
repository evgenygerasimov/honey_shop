package org.site.honey_shop.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.site.honey_shop.dto.UserResponseDTO;
import org.site.honey_shop.entity.Category;
import org.site.honey_shop.service.CategoryService;
import org.site.honey_shop.service.UserService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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
                .andExpect(redirectedUrl("/categories"));

        verify(categoryService, times(1)).saveCategoryByName("New Category");
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
                .when(categoryService).saveCategoryByName("Existing Category");

        mockMvc.perform(post("/categories")
                        .param("name", "Existing Category"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/categories/new"))
                .andExpect(flash().attribute("errorMessage", "Category already exists"));
    }



    @Test
    void testShowCategories() throws Exception {
        when(userService.findByUsername("admin")).thenReturn(user);
        when(categoryService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(view().name("all-categories"))
                .andExpect(model().attributeExists("userId"))
                .andExpect(model().attributeExists("categories"));
    }

    @Test
    void testUpdateCategoryName() throws Exception {
        UUID id = UUID.randomUUID();
        var category = new Category();
        when(categoryService.findById(id)).thenReturn(category);

        mockMvc.perform(post("/categories/update-category-name/" + id)
                        .param("name", "Updated Name"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/categories"));

        verify(categoryService).updateCategoryName(category);
        assert category.getName().equals("Updated Name");
    }

    @Test
    void testShowEditCategoryForm() throws Exception {
        UUID id = UUID.randomUUID();
        var category = new Category();
        when(categoryService.findById(id)).thenReturn(category);
        when(userService.findByUsername("admin")).thenReturn(user);

        mockMvc.perform(get("/categories/edit/" + id))
                .andExpect(status().isOk())
                .andExpect(view().name("edit-category"))
                .andExpect(model().attributeExists("userId"))
                .andExpect(model().attributeExists("category"));
    }

    @Test
    void testDeleteCategory() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(post("/categories/delete/" + id))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/categories"));

        verify(categoryService).deleteCategory(id);
    }

    @Test
    void testDeleteImage_Success() throws Exception {
        File tempFile = File.createTempFile("test-image", ".jpg");
        String imagePath = tempFile.getAbsolutePath();
        String categoryId = UUID.randomUUID().toString();

        mockMvc.perform(post("/categories/delete-image")
                        .param("imageFilename", imagePath)
                        .param("categoryId", categoryId))
                .andExpect(status().isOk())
                .andExpect(content().string("Photo from category was deleted successfully."));

        verify(categoryService).removeImageFromCategory(UUID.fromString(categoryId));
    }

    @Test
    void testDeleteImage_Failure() throws Exception {
        File file = new File("non-existent.jpg");
        String categoryId = UUID.randomUUID().toString();

        mockMvc.perform(post("/categories/delete-image")
                        .param("imageFilename", file.getAbsolutePath())
                        .param("categoryId", categoryId))
                .andExpect(status().isOk()) // File doesn't exist, still returns OK
                .andExpect(content().string("Photo from category was deleted successfully."));

        verify(categoryService).removeImageFromCategory(UUID.fromString(categoryId));
    }

    @Test
    void testFullUpdateCategory() throws Exception {
        var image = mock(MultipartFile.class);
        var category = new Category();
        category.setCategoryId(UUID.randomUUID());

        when(image.isEmpty()).thenReturn(true); // если образ не обязателен

        mockMvc = MockMvcBuilders.standaloneSetup(categoryController).build();

        mockMvc.perform(multipart("/categories/full-update-category")
                        .file("image", new byte[0])
                        .flashAttr("category", category))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/categories"));

        verify(categoryService).fullUpdateCategory(any(Category.class), any(MultipartFile.class));
    }

}
