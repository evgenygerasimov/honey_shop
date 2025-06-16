package org.site.honey_shop.service;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.site.honey_shop.entity.Category;
import org.site.honey_shop.entity.Product;
import org.site.honey_shop.exception.ImageUploadException;
import org.site.honey_shop.repository.CategoryRepository;
import org.site.honey_shop.repository.ProductRepository;
import org.springframework.data.domain.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        categoryService.UPLOAD_DIRECTORY = "/tmp/uploads";
    }

    private final String existingCategoryName = "Гречишный";
    private final String newCategoryName = "Разнотравье";

    private final UUID categoryId = UUID.randomUUID();

    @Test
    void testSaveCategoryWithImage_NewCategory_Success() throws IOException {
        Category inputCategory = Category.builder().name(newCategoryName).visible(true).build();
        MultipartFile image = mock(MultipartFile.class);
        when(image.isEmpty()).thenReturn(false);
        when(image.getOriginalFilename()).thenReturn("image.png");
        when(image.getBytes()).thenReturn(new byte[]{1, 2, 3});

        when(categoryRepository.existsByName(newCategoryName)).thenReturn(false);

        Category savedCategory = Category.builder()
                .name(newCategoryName)
                .imageUrl("/assets/img/image.png")
                .visible(true)
                .build();
        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);

        Category result = categoryService.saveCategoryWithImage(inputCategory, image);

        assertEquals(newCategoryName, result.getName());
        assertEquals("/assets/img/image.png", result.getImageUrl());
        verify(categoryRepository).existsByName(newCategoryName);
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void testSaveCategoryWithImage_ExistingName_Throws() {
        Category inputCategory = Category.builder().name(existingCategoryName).visible(true).build();
        MultipartFile image = mock(MultipartFile.class);

        when(categoryRepository.existsByName(existingCategoryName)).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> categoryService.saveCategoryWithImage(inputCategory, image));

        assertEquals("Категория с таким названием уже существует!", ex.getMessage());
        verify(categoryRepository).existsByName(existingCategoryName);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void testUpdateCategoryName_ExistingCategory_Success() {
        Category updatedCategory = Category.builder()
                .categoryId(categoryId)
                .name("Новое имя")
                .build();

        Category existingCategory = Category.builder()
                .categoryId(categoryId)
                .name("Старое имя")
                .build();

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(existingCategory));
        when(categoryRepository.save(any(Category.class))).thenAnswer(i -> i.getArgument(0));

        Category result = categoryService.updateCategoryName(updatedCategory);

        assertEquals("Новое имя", result.getName());
        verify(categoryRepository).findById(categoryId);
        verify(categoryRepository).save(existingCategory);
    }

    @Test
    void testUpdateCategoryName_NotFound_Throws() {
        Category updatedCategory = Category.builder()
                .categoryId(categoryId)
                .name("Новое имя")
                .build();

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> categoryService.updateCategoryName(updatedCategory));
        assertEquals("Category not found", ex.getMessage());
        verify(categoryRepository).findById(categoryId);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void testFullUpdateCategory_WithImage_Success() throws IOException {
        MultipartFile image = mock(MultipartFile.class);
        when(image.isEmpty()).thenReturn(false);
        when(image.getOriginalFilename()).thenReturn("updated.png");
        when(image.getBytes()).thenReturn(new byte[]{1, 2, 3});

        Category inputCategory = Category.builder()
                .categoryId(categoryId)
                .name("Новое имя")
                .visible(true)
                .build();

        Category existingCategory = Category.builder()
                .categoryId(categoryId)
                .name("Старое имя")
                .imageUrl("/assets/img/old.png")
                .visible(false)
                .build();

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(existingCategory));
        when(categoryRepository.save(any(Category.class))).thenAnswer(i -> i.getArgument(0));

        Category result = categoryService.fullUpdateCategory(inputCategory, image);

        assertEquals("Новое имя", result.getName());
        assertEquals("/assets/img/updated.png", result.getImageUrl());
        assertTrue(result.getVisible());
        verify(categoryRepository).findById(categoryId);
        verify(categoryRepository).save(existingCategory);
    }

    @Test
    void testFullUpdateCategory_WithEmptyImage_Success() throws IOException {
        MultipartFile image = mock(MultipartFile.class);
        when(image.isEmpty()).thenReturn(true);

        Category inputCategory = Category.builder()
                .categoryId(categoryId)
                .name("Новое имя")
                .visible(true)
                .build();

        Category existingCategory = Category.builder()
                .categoryId(categoryId)
                .name("Старое имя")
                .imageUrl("/assets/img/old.png")
                .visible(false)
                .build();

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(existingCategory));
        when(categoryRepository.save(any(Category.class))).thenAnswer(i -> i.getArgument(0));

        Category result = categoryService.fullUpdateCategory(inputCategory, image);

        assertEquals("Новое имя", result.getName());
        assertEquals("/assets/img/old.png", result.getImageUrl());
        assertTrue(result.getVisible());
    }

    @Test
    void testFindAll_ReturnsSortedList() {
        List<Category> categories = List.of(
                Category.builder().name("Б").build(),
                Category.builder().name("А").build()
        );
        when(categoryRepository.findAll(Sort.by("name"))).thenReturn(categories);

        List<Category> result = categoryService.findAll();

        assertEquals(2, result.size());
        verify(categoryRepository).findAll(Sort.by("name"));
    }

    @Test
    void testFindAllWithPagination_ReturnsPage() {
        Pageable pageable = PageRequest.of(0, 5);
        List<Category> categories = List.of(Category.builder().name("А").build());
        Page<Category> page = new PageImpl<>(categories, pageable, 1);
        when(categoryRepository.findAll(pageable)).thenReturn(page);

        Page<Category> result = categoryService.findAll(pageable);

        assertEquals(1, result.getTotalElements());
        verify(categoryRepository).findAll(pageable);
    }

    @Test
    void testFindAllTrueVisibleCategories_ReturnsVisible() {
        List<Category> visibleCategories = List.of(
                Category.builder().name("Видимая").build()
        );
        when(categoryRepository.findAllByVisibleTrueOrderByShowcaseOrderAsc()).thenReturn(visibleCategories);

        List<Category> result = categoryService.findAllTrueVisibleCategories();

        assertEquals(1, result.size());
        verify(categoryRepository).findAllByVisibleTrueOrderByShowcaseOrderAsc();
    }

    @Test
    void testFindById_ExistingCategory_ReturnsCategory() {
        Category category = Category.builder().categoryId(categoryId).name(existingCategoryName).build();
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        Category result = categoryService.findById(categoryId);

        assertNotNull(result);
        assertEquals(existingCategoryName, result.getName());
        verify(categoryRepository).findById(categoryId);
    }

    @Test
    void testFindById_NonExistingCategory_ReturnsNull() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        Category result = categoryService.findById(categoryId);

        assertNull(result);
        verify(categoryRepository).findById(categoryId);
    }

    @Test
    void testFindByName_ReturnsCategory() {
        Category category = Category.builder().name(existingCategoryName).build();
        when(categoryRepository.findByName(existingCategoryName)).thenReturn(category);

        Category result = categoryService.findByName(existingCategoryName);

        assertEquals(existingCategoryName, result.getName());
        verify(categoryRepository).findByName(existingCategoryName);
    }

    @Test
    void testDeleteCategory_Existing_Success() {
        Category category = Category.builder().categoryId(categoryId).name(existingCategoryName).build();
        Product product1 = Product.builder().name("Продукт1").category(category).build();
        Product product2 = Product.builder().name("Продукт2").category(category).build();
        List<Product> products = List.of(product1, product2);

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(productRepository.findAllByCategory(category)).thenReturn(products);

        categoryService.deleteCategory(categoryId);

        assertNull(product1.getCategory());
        assertNull(product2.getCategory());

        verify(productRepository).saveAll(products);
        verify(categoryRepository).delete(category);
    }

    @Test
    void testDeleteCategory_NotFound_Throws() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> categoryService.deleteCategory(categoryId));

        assertEquals("Категория не найдена", ex.getMessage());
        verify(categoryRepository).findById(categoryId);
        verify(productRepository, never()).saveAll(any());
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void testImageSelectionProcessing_WithValidImage() throws IOException {
        MultipartFile image = mock(MultipartFile.class);
        when(image.isEmpty()).thenReturn(false);
        when(image.getOriginalFilename()).thenReturn("test.png");
        when(image.getBytes()).thenReturn(new byte[]{1, 2, 3});

        String url = categoryService.imageSelectionProcessing(image);

        assertEquals("/assets/img/test.png", url);
    }

    @Test
    void testImageSelectionProcessing_WithEmptyImage_ReturnsEmptyString() {
        MultipartFile image = mock(MultipartFile.class);
        when(image.isEmpty()).thenReturn(true);

        String url = categoryService.imageSelectionProcessing(image);

        assertEquals("", url);
    }

    @Test
    void testImageSelectionProcessing_WithIOException_Throws() throws IOException {
        MultipartFile image = mock(MultipartFile.class);
        when(image.isEmpty()).thenReturn(false);
        when(image.getOriginalFilename()).thenReturn("file.png");
        when(image.getBytes()).thenThrow(new IOException("fail"));

        ImageUploadException ex = assertThrows(ImageUploadException.class,
                () -> categoryService.imageSelectionProcessing(image));
        assertEquals("Ошибка загрузки изображения!", ex.getMessage());
    }

    @Test
    void testRemoveImageFromCategory_Success() {
        Category category = Category.builder().categoryId(categoryId).imageUrl("/assets/img/old.png").build();
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        boolean result = categoryService.removeImageFromCategory(categoryId);

        assertTrue(result);
        assertNull(category.getImageUrl());
        verify(categoryRepository).save(category);
    }

    @Test
    void testRemoveImageFromCategory_NotFound() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        assertThrows(NullPointerException.class,
                () -> categoryService.removeImageFromCategory(categoryId));
        verify(categoryRepository).findById(categoryId);
    }
}
