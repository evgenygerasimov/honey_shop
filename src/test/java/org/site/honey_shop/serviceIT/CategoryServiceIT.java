package org.site.honey_shop.serviceIT;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.site.honey_shop.TestContainerConfig;
import org.site.honey_shop.entity.Category;
import org.site.honey_shop.entity.Product;
import org.site.honey_shop.repository.CategoryRepository;
import org.site.honey_shop.repository.ProductRepository;
import org.site.honey_shop.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class CategoryServiceIT extends TestContainerConfig {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void clearDb() {
        productRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    void testSaveCategoryWithImage_success() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image", "honey.jpg", "image/jpeg", "some-image-data".getBytes());

        Category categoryToSave = Category.builder()
                .name("Пыльца")
                .visible(true)
                .build();

        Category savedCategory = categoryService.saveCategoryWithImage(categoryToSave, image);

        assertThat(savedCategory.getCategoryId()).isNotNull();
        assertThat(savedCategory.getName()).isEqualTo("Пыльца");
        assertThat(savedCategory.getImageUrl()).isNotBlank();
        assertThat(savedCategory.getImageUrl()).contains("honey.jpg");
    }

    @Test
    void testSaveCategoryWithImage_duplicateName_throwsException() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image", "image.jpg", "image/jpeg", "image".getBytes());

        Category categoryToSave = Category.builder()
                .name("Пыльца")
                .visible(true)
                .build();

        categoryService.saveCategoryWithImage(categoryToSave, image);

        Category duplicate = Category.builder()
                .name("Пыльца")
                .visible(true)
                .build();

        assertThatThrownBy(() -> categoryService.saveCategoryWithImage(duplicate, image))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Категория с таким названием уже существует!");
    }

    @Test
    void testUpdateCategoryName_success() throws Exception {
        MockMultipartFile image = new MockMultipartFile("image", "a.jpg", "image/jpeg", "img".getBytes());
        Category saved = categoryService.saveCategoryWithImage(Category.builder().name("Старое имя").visible(true).build(), image);

        Category updateData = Category.builder()
                .categoryId(saved.getCategoryId())
                .name("Новое имя")
                .build();

        Category updated = categoryService.updateCategoryName(updateData);

        assertThat(updated.getName()).isEqualTo("Новое имя");
        assertThat(categoryRepository.findById(saved.getCategoryId()))
                .get()
                .extracting(Category::getName)
                .isEqualTo("Новое имя");
    }

    @Test
    void testUpdateCategoryName_nonExistingCategory_throwsException() {
        Category updateData = Category.builder()
                .categoryId(UUID.randomUUID())
                .name("Новое имя")
                .build();

        assertThatThrownBy(() -> categoryService.updateCategoryName(updateData))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Category not found");
    }

    @Test
    void testFullUpdateCategory_withImage() throws Exception {
        MockMultipartFile image = new MockMultipartFile("image", "a.jpg", "image/jpeg", "img".getBytes());
        Category saved = categoryService.saveCategoryWithImage(Category.builder().name("Прополис").visible(true).build(), image);

        MockMultipartFile newImage = new MockMultipartFile("image", "new.jpg", "image/jpeg", "new-img".getBytes());

        Category updateData = Category.builder()
                .categoryId(saved.getCategoryId())
                .name("Прополис Обновлённый")
                .visible(false)
                .build();

        Category updated = categoryService.fullUpdateCategory(updateData, newImage);

        assertThat(updated.getName()).isEqualTo("Прополис Обновлённый");
        assertThat(updated.getVisible()).isFalse();
        assertThat(updated.getImageUrl()).contains("new.jpg");
    }

    @Test
    void testFindAllCategories_sortedByName() throws Exception {
        categoryService.saveCategoryWithImage(Category.builder().name("Мёд").visible(true).build(),
                new MockMultipartFile("image", "a.jpg", "image/jpeg", "a".getBytes()));
        categoryService.saveCategoryWithImage(Category.builder().name("Пыльца").visible(true).build(),
                new MockMultipartFile("image", "b.jpg", "image/jpeg", "b".getBytes()));

        List<Category> categories = categoryService.findAll();

        assertThat(categories).hasSize(2);
        assertThat(categories).extracting(Category::getName)
                .containsExactly("Мёд", "Пыльца"); // алфавитный порядок
    }

    @Test
    void testFindAllTrueVisibleCategories() {
        categoryRepository.save(Category.builder().name("Видимая").visible(true).showcaseOrder(1).build());
        categoryRepository.save(Category.builder().name("Невидимая").visible(false).showcaseOrder(2).build());

        List<Category> visible = categoryService.findAllTrueVisibleCategories();

        assertThat(visible).hasSize(1);
        assertThat(visible.get(0).getName()).isEqualTo("Видимая");
    }

    @Test
    void testFindById_existingAndNonExisting() throws Exception {
        Category saved = categoryService.saveCategoryWithImage(Category.builder().name("Прополис").visible(true).build(),
                new MockMultipartFile("image", "a.jpg", "image/jpeg", "img".getBytes()));

        Category found = categoryService.findById(saved.getCategoryId());

        assertThat(found).isNotNull();
        assertThat(found.getCategoryId()).isEqualTo(saved.getCategoryId());

        Category notFound = categoryService.findById(UUID.randomUUID());
        assertThat(notFound).isNull();
    }

    @Test
    void testFindByName_existingAndNonExisting() throws Exception {
        categoryService.saveCategoryWithImage(Category.builder().name("Прополис").visible(true).build(),
                new MockMultipartFile("image", "a.jpg", "image/jpeg", "img".getBytes()));

        Category found = categoryService.findByName("Прополис");
        assertThat(found).isNotNull();

        Category notFound = categoryService.findByName("Несуществующая");
        assertThat(notFound).isNull();
    }

    @Test
    void testDeleteCategory_removesCategoryAndUnlinksProducts() throws Exception {
        Category category = categoryService.saveCategoryWithImage(
                Category.builder().name("Удаляемая категория").visible(true).build(),
                new MockMultipartFile("image", "a.jpg", "image/jpeg", "img".getBytes()));

        Product p1 = createValidProduct("Продукт 1", category);
        Product p2 = createValidProduct("Продукт 2", category);
        productRepository.saveAll(List.of(p1, p2));

        categoryService.deleteCategory(category.getCategoryId());

        assertThat(categoryRepository.findById(category.getCategoryId())).isEmpty();

        List<Product> productsAfter = productRepository.findAll();
        assertThat(productsAfter).hasSize(2);
        assertThat(productsAfter).allSatisfy(p -> assertThat(p.getCategory()).isNull());
    }


    @Test
    void testDeleteCategory_nonExisting_throwsException() {
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> categoryService.deleteCategory(id))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Категория не найдена");
    }

    @Test
    void testRemoveImageFromCategory() throws Exception {
        Category saved = categoryService.saveCategoryWithImage(Category.builder().name("С картинкой").visible(true).build(),
                new MockMultipartFile("image", "img.jpg", "image/jpeg", "img".getBytes()));

        boolean result = categoryService.removeImageFromCategory(saved.getCategoryId());

        assertThat(result).isTrue();

        Category updated = categoryRepository.findById(saved.getCategoryId()).orElseThrow();
        assertThat(updated.getImageUrl()).isNull();
    }

    private Product createValidProduct(String name, Category category) {
        return Product.builder()
                .name(name)
                .shortDescription("Краткое описание")
                .description("Полное описание")
                .price(new BigDecimal("100.00"))
                .length(10.0)
                .width(5.0)
                .height(2.0)
                .weight(1.0)
                .stockQuantity(50)
                .images(List.of("image1.jpg", "image2.jpg"))
                .category(category)
                .showInShowcase(true)
                .showcaseOrder(1)
                .build();
    }
}
