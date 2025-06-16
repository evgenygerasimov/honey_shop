package org.site.honey_shop.serviceIT;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.site.honey_shop.TestContainerConfig;
import org.site.honey_shop.entity.Category;
import org.site.honey_shop.entity.Product;
import org.site.honey_shop.exception.DeleteProductException;
import org.site.honey_shop.repository.CategoryRepository;
import org.site.honey_shop.repository.ProductRepository;
import org.site.honey_shop.service.CategoryService;
import org.site.honey_shop.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class ProductServiceIT extends TestContainerConfig {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category category;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        MockMultipartFile image = new MockMultipartFile(
                "image", "honey.jpg", "image/jpeg", "some-image-data".getBytes());

        category = Category.builder()
                .name("Пыльца")
                .visible(true)
                .build();

        categoryService.saveCategoryWithImage(category, image);
    }

    private Product buildSampleProduct() {
        return Product.builder()
                .name("Продукт 1")
                .description("Описание продукта")
                .shortDescription("Кратко")
                .price(new BigDecimal("10.00"))
                .length(10.0)
                .width(5.0)
                .height(3.0)
                .weight(0.5)
                .images(new java.util.ArrayList<>())
                .category(category)
                .stockQuantity(100)
                .build();
    }


    @Test
    @Transactional
    void testCreateProduct_success() {
        Product product = buildSampleProduct();

        List<MultipartFile> images = List.of(new MockMultipartFile("image", "image.jpg", "image/jpeg", "test image".getBytes(StandardCharsets.UTF_8)));
        productService.createProduct(product,   images, "0");

        List<Product> products = productRepository.findAll();
        assertThat(products).hasSize(1);
        Product saved = products.getFirst();
        assertThat(saved.getName()).isEqualTo("Продукт 1");
        assertThat(saved.getImages()).hasSize(1);
    }

    @Test
    void testGetProductById_existingProduct() {
        Product product = buildSampleProduct();
        MockMultipartFile image = new MockMultipartFile("image", "image.jpg", "image/jpeg", "test image".getBytes(StandardCharsets.UTF_8));
        productService.createProduct(product, List.of(image), "0");
        UUID id = productRepository.findAll().getFirst().getProductId();

        Product fromDb = productService.getProductById(id);

        assertThat(fromDb).isNotNull();
        assertThat(fromDb.getName()).isEqualTo("Продукт 1");
    }

    @Test
    void testGetProductById_notFound_throwsException() {
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> productService.getProductById(id))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    void testGetAllProducts_sortedByPriceAndGrouped() {
        Product p1 = buildSampleProduct();
        p1.setPrice(new BigDecimal("15.00"));
        Product p2 = buildSampleProduct();
        p2.setName("Продукт 2");
        p2.setPrice(new BigDecimal("5.00"));

        MockMultipartFile image1 = new MockMultipartFile("image", "image.jpg", "image/jpeg", "test image".getBytes(StandardCharsets.UTF_8));
        productService.createProduct(p1, List.of(image1), "0");
        MockMultipartFile image2 = new MockMultipartFile("image", "image.jpg", "image/jpeg", "test image".getBytes(StandardCharsets.UTF_8));
        productService.createProduct(p2, List.of(image2), "0");

        Page<Product> page = productService.getAllProducts(PageRequest.of(0, 10));

        assertThat(page).isNotNull();
        assertThat(page.getContent()).isNotEmpty();
    }

    @Test
    void testUpdateProduct_success() {
        Product product = buildSampleProduct();
        MockMultipartFile image = new MockMultipartFile("image", "image.jpg", "image/jpeg", "test image".getBytes(StandardCharsets.UTF_8));
        productService.createProduct(product, List.of(image), "0");
        Product fromDb = productRepository.findAll().getFirst();

        fromDb.setName("Обновлённый продукт");
        Product updated = productService.updateProduct(fromDb, Collections.emptyList(), "");

        assertThat(updated.getName()).isEqualTo("Обновлённый продукт");
    }

    @Test
    void testDeleteProduct_success() {
        Product product = buildSampleProduct();
        MockMultipartFile image = new MockMultipartFile("image", "image.jpg", "image/jpeg", "test image".getBytes(StandardCharsets.UTF_8));
        productService.createProduct(product, List.of(image), "0");
        UUID id = productRepository.findAll().getFirst().getProductId();

        productService.deleteProduct(id);

        assertThat(productRepository.findById(id)).isEmpty();
    }

    @Test
    void testDeleteProduct_failure_throwsDeleteProductException() {
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> productService.deleteProduct(id))
                .isInstanceOf(DeleteProductException.class)
                .hasMessageContaining("Продукт с id=" + id + " не найден, удаление невозможно.");
    }

    @Test
    void testUpdateStockForReduction() {
        Product product = buildSampleProduct();
        MockMultipartFile image = new MockMultipartFile("image", "image.jpg", "image/jpeg", "test image".getBytes(StandardCharsets.UTF_8));
        productService.createProduct(product, List.of(image), "0");
        Product fromDb = productRepository.findAll().getFirst();

        productService.updateStockForReduction(fromDb, 10);

        Product updated = productRepository.findById(fromDb.getProductId()).get();
        assertThat(updated.getStockQuantity()).isEqualTo(90);
    }

    @Test
    void testUpdateStockForAddition() {
        Product product = buildSampleProduct();
        MockMultipartFile image = new MockMultipartFile("image", "image.jpg", "image/jpeg", "test image".getBytes(StandardCharsets.UTF_8));
        productService.createProduct(product, List.of(image), "0");
        Product fromDb = productRepository.findAll().getFirst();

        productService.updateStockForAddition(fromDb, 5);

        Product updated = productRepository.findById(fromDb.getProductId()).get();
        assertThat(updated.getStockQuantity()).isEqualTo(105);
    }

    @Test
    @Transactional
    void testRemoveImageFromProduct() {
        Product product = buildSampleProduct();
        MockMultipartFile image = new MockMultipartFile("image", "image.jpg", "image/jpeg", "test image".getBytes(StandardCharsets.UTF_8));
        productService.createProduct(product, List.of(image), "0");
        Product saved = productRepository.findAll().getFirst();

        productService.removeImageFromProduct(saved.getProductId(), "/tmp/images/image.jpg");

        Product updated = productRepository.findById(saved.getProductId()).get();
        assertThat(updated.getImages()).doesNotContain("/tmp/images/image.jpg");
    }
}
