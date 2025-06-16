package org.site.honey_shop.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.site.honey_shop.dto.UserResponseDTO;
import org.site.honey_shop.entity.Category;
import org.site.honey_shop.service.CategoryService;
import org.site.honey_shop.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
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
                null, null, null, null, null
        );

        when(userService.findByUsername("admin")).thenReturn(user);

        mockMvc = MockMvcBuilders
                .standaloneSetup(categoryController)
                .build();
    }

    @Test
    void testShowCategoryForm() throws Exception {
        mockMvc.perform(get("/categories/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("add-category"))
                .andExpect(model().attributeExists("authUserId"))
                .andExpect(model().attributeExists("category"));
    }

    @Test
    void testFullCreateCategory_Valid() throws Exception {
        mockMvc.perform(multipart("/categories/create-with-image")
                        .file("image", new byte[0])
                        .param("name", "New Category")
                        .flashAttr("category", new Category()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/categories"));

        verify(categoryService).saveCategoryWithImage(any(Category.class), any(MultipartFile.class));
    }

    @Test
    void testFullCreateCategory_WithValidationError() throws Exception {
        mockMvc.perform(multipart("/categories/create-with-image")
                        .file("image", new byte[0])
                        .param("name", "")
                        .flashAttr("category", new Category()))
                .andExpect(status().isOk())
                .andExpect(view().name("add-category"))
                .andExpect(model().attributeExists("authUserId"));
    }

    @Test
    void testFullCreateCategory_DuplicateNameThrowsException() throws Exception {
        doThrow(new IllegalArgumentException("Category already exists"))
                .when(categoryService).saveCategoryWithImage(any(Category.class), any(MultipartFile.class));

        Category category = new Category();
        category.setName("Existing Category");

        mockMvc.perform(multipart("/categories/create-with-image")
                        .file("image", new byte[0])
                        .flashAttr("category", category)
                        .param("name", category.getName()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/categories/new"))
                .andExpect(flash().attribute("errorMessage", "Category already exists"));
    }

    @Test
    void testShowCategories() throws Exception {
        Page<Category> emptyPage = new PageImpl<>(List.of());
        when(categoryService.findAll(any(Pageable.class))).thenReturn(emptyPage);

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(view().name("all-categories"))
                .andExpect(model().attributeExists("authUserId"))
                .andExpect(model().attributeExists("categoriesPage"));
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

        mockMvc.perform(get("/categories/edit/" + id))
                .andExpect(status().isOk())
                .andExpect(view().name("edit-category"))
                .andExpect(model().attributeExists("authUserId"))
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
    void testDeleteImage_FileNotFound() throws Exception {
        File file = new File("non-existent.jpg");
        String categoryId = UUID.randomUUID().toString();

        mockMvc.perform(post("/categories/delete-image")
                        .param("imageFilename", file.getAbsolutePath())
                        .param("categoryId", categoryId))
                .andExpect(status().isOk())
                .andExpect(content().string("Photo from category was deleted successfully."));

        verify(categoryService).removeImageFromCategory(UUID.fromString(categoryId));
    }

    @Test
    void testFullUpdateCategory() throws Exception {
        var category = new Category();
        category.setCategoryId(UUID.randomUUID());
        category.setName("Test Category");

        when(categoryService.fullUpdateCategory(any(Category.class), any(MultipartFile.class)))
                .thenReturn(category);

        mockMvc.perform(multipart("/categories/full-update-category")
                        .file("image", new byte[0])
                        .param("name", category.getName())
                        .param("categoryId", category.getCategoryId().toString())
                        .flashAttr("category", category))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/categories"));

        verify(categoryService).fullUpdateCategory(any(Category.class), any(MultipartFile.class));
    }
}
