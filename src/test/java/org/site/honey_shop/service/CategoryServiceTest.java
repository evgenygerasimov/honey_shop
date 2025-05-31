package org.site.honey_shop.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.site.honey_shop.entity.Category;
import org.site.honey_shop.repository.CategoryRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    private final String existingCategoryName = "Гречишный";

    private final String newCategoryName = "Разнотравье";

    @Test
    void testSave_CategoryWithImage_NewCategory_Success() {
        when(categoryRepository.existsByName(newCategoryName)).thenReturn(false);

        Category savedCategory = Category.builder().name(newCategoryName).build();
        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);

        Category result = categoryService.saveCategoryByName(newCategoryName);

        assertEquals(newCategoryName, result.getName());
        verify(categoryRepository).existsByName(newCategoryName);
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void testSave_CategoryWithImage_ExistingCategory_ThrowsException() {
        when(categoryRepository.existsByName(existingCategoryName)).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> categoryService.saveCategoryByName(existingCategoryName)
        );

        assertEquals("Категория с таким названием уже существует!", exception.getMessage());
        verify(categoryRepository).existsByName(existingCategoryName);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void testFindAll_ReturnsList() {
        List<Category> categories = List.of(
                Category.builder().name("Гречишный").build(),
                Category.builder().name("Разнотравье").build()
        );
        when(categoryRepository.findAll()).thenReturn(categories);

        List<Category> result = categoryService.findAll();

        assertEquals(2, result.size());
        verify(categoryRepository).findAll();
    }

    @Test
    void testFindByName_ReturnsCategory() {
        Category category = Category.builder().name(existingCategoryName).build();
        when(categoryRepository.findByName(existingCategoryName)).thenReturn(category);

        Category result = categoryService.findByName(existingCategoryName);

        assertEquals(existingCategoryName, result.getName());
        verify(categoryRepository).findByName(existingCategoryName);
    }
}
