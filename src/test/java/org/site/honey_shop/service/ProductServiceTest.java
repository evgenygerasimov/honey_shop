package org.site.honey_shop.service;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.site.honey_shop.entity.Category;
import org.site.honey_shop.entity.Product;
import org.site.honey_shop.exception.DeleteProductException;
import org.site.honey_shop.exception.OrderCreateException;
import org.site.honey_shop.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @InjectMocks
    private ProductService productService;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        try {
            var uploadDir = productService.getClass().getDeclaredField("UPLOAD_DIRECTORY");
            uploadDir.setAccessible(true);
            uploadDir.set(productService, "uploads");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void getProductById_shouldReturnProduct() {
        UUID id = UUID.randomUUID();
        Product product = Product.builder().productId(id).name("Honey").build();
        when(productRepository.findById(id)).thenReturn(Optional.of(product));

        Product result = productService.getProductById(id);

        assertEquals("Honey", result.getName());
        verify(productRepository).findById(id);
    }

    @Test
    void getProductById_shouldThrowIfNotFound() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> productService.getProductById(id));
    }

    @Test
    void getAllProducts_shouldReturnPage() {
        Page<Product> page = new PageImpl<>(List.of(Product.builder().name("P1").build()));
        Pageable pageable = mock(Pageable.class);
        when(productRepository.findAll(pageable)).thenReturn(page);

        Page<Product> result = productService.getAllProducts(pageable);

        assertEquals(1, result.getTotalElements());
        verify(productRepository).findAll(pageable);
    }

    @Test
    void deleteProduct_shouldDelete() {
        UUID id = UUID.randomUUID();
        when(productRepository.existsById(id)).thenReturn(true);

        productService.deleteProduct(id);

        verify(productRepository).deleteById(id);
    }

    @Test
    void deleteProduct_shouldThrowCustomException() {
        UUID id = UUID.randomUUID();
        when(productRepository.existsById(id)).thenReturn(true);
        doThrow(new RuntimeException()).when(productRepository).deleteById(id);

        assertThrows(DeleteProductException.class, () -> productService.deleteProduct(id));
    }

    @Test
    void updateStockForReduction_shouldDecreaseStock() {
        Product product = Product.builder().stockQuantity(10).name("Honey").build();
        productService.updateStockForReduction(product, 3);
        assertEquals(7, product.getStockQuantity());
        verify(productRepository).save(product);
    }

    @Test
    void updateStockForReduction_shouldThrowIfInsufficient() {
        Product product = Product.builder().stockQuantity(2).name("Honey").build();
        assertThrows(OrderCreateException.class, () -> productService.updateStockForReduction(product, 3));
        verify(productRepository, never()).save(any());
    }

    @Test
    void updateStockForAddition_shouldIncreaseStock() {
        Product product = Product.builder().stockQuantity(10).name("Honey").build();
        productService.updateStockForAddition(product, 5);
        assertEquals(15, product.getStockQuantity());
        verify(productRepository).save(product);
    }

    @Test
    void removeImageFromProduct_shouldRemoveImage() {
        UUID id = UUID.randomUUID();
        List<String> images = new ArrayList<>(List.of("img1.jpg", "img2.jpg"));
        Product product = Product.builder().productId(id).images(images).build();
        when(productRepository.findById(id)).thenReturn(Optional.of(product));

        productService.removeImageFromProduct(id, "img1.jpg");

        assertFalse(product.getImages().contains("img1.jpg"));
        verify(productRepository).save(product);
    }

    @Test
    void createProduct_shouldSaveProductWithOrderedImages() {
        Category category = Category.builder().name("Honey").build();

        Product product = Product.builder()
                .name("Test")
                .description("desc")
                .shortDescription("short")
                .price(new BigDecimal(100))
                .length(10.0)
                .width(10.0)
                .height(10.0)
                .weight(500.0)
                .stockQuantity(10)
                .category(category)
                .images(new ArrayList<>())
                .build();

        MockMultipartFile image1 = new MockMultipartFile("file", "image1.jpg", "image/jpeg", "data1".getBytes());
        MockMultipartFile image2 = new MockMultipartFile("file", "image2.jpg", "image/jpeg", "data2".getBytes());
        List<MultipartFile> images = List.of(image1, image2);

        when(categoryService.findByName("Honey")).thenReturn(category);

        productService.createProduct(product, images, "1,0");

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        Product saved = captor.getValue();

        assertEquals("Test", saved.getName());
        assertEquals(2, saved.getImages().size());
        assertTrue(saved.getImages().get(0).contains("image2.jpg"));
    }

    @Test
    void updateProduct_shouldUpdateExistingProductWithImageOrder() {
        UUID id = UUID.randomUUID();
        Category category = Category.builder().name("Honey").build();
        Product existing = Product.builder()
                .productId(id)
                .images(new ArrayList<>(List.of("old.jpg")))
                .build();
        Product input = Product.builder()
                .productId(id)
                .name("Updated")
                .description("desc")
                .shortDescription("short")
                .price(new BigDecimal(100))
                .length(10.0)
                .width(10.0)
                .height(10.0)
                .weight(500.0)
                .stockQuantity(10)
                .category(category)
                .showInShowcase(true)
                .build();

        when(productRepository.findById(id)).thenReturn(Optional.of(existing));
        when(categoryService.findByName("Honey")).thenReturn(category);

        productService.updateProduct(input, Collections.emptyList(), "new1.jpg,new2.jpg");

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        Product updated = captor.getValue();

        assertEquals("Updated", updated.getName());
        assertEquals(List.of("uploads/new1.jpg", "uploads/new2.jpg"), updated.getImages());
    }
}
