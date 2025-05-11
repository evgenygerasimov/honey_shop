package org.site.honey_shop.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.site.honey_shop.dto.UserResponseDTO;
import org.site.honey_shop.entity.Category;
import org.site.honey_shop.entity.Product;
import org.site.honey_shop.exception.DeleteProductException;
import org.site.honey_shop.exception.ProductCreationException;
import org.site.honey_shop.service.CategoryService;
import org.site.honey_shop.service.ProductService;
import org.site.honey_shop.service.UserService;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ProductService productService;

    @Mock
    private CategoryService categoryService;

    @Mock
    private UserService userService;

    @InjectMocks
    private ProductController productController;

    @Mock
    private Principal principal;

    private Product product;
    private MultipartFile picture;

    @BeforeEach
    void setUp() {
        var viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/views/");
        viewResolver.setSuffix(".html");

        mockMvc = MockMvcBuilders
                .standaloneSetup(productController)
                .setViewResolvers(viewResolver)
                .build();

        var authentication = new UsernamePasswordAuthenticationToken("admin", null);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        product = Product.builder()
                .name("Мёд липовый")
                .shortDescription("Натуральный липовый мёд с пасеки.")
                .description("Полностью натуральный липовый мёд, собранный в экологически чистом районе.")
                .price(new BigDecimal("1500.00"))
                .length(10.0)
                .width(10.0)
                .height(12.0)
                .weight(0.5)
                .stockQuantity(20)
                .category(new Category())
                .build();

        picture = new MockMultipartFile(
                "pictures",
                "image.jpg",
                "image/jpeg",
                new byte[]{}
        );
    }

    @Test
    void testListProducts() throws Exception {
        var userDto = mock(UserResponseDTO.class);
        when(userDto.userId()).thenReturn(UUID.randomUUID());
        when(userService.findByUsername("admin")).thenReturn(userDto);

        when(productService.getAllProducts()).thenReturn(List.of(new Product()));

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(view().name("all-products"));

        verify(productService).getAllProducts();
    }

    @Test
    void testShowProduct() throws Exception {
        UUID productId = UUID.randomUUID();
        Product product = new Product();
        when(productService.getProductById(productId)).thenReturn(product);

        mockMvc.perform(get("/products/{productId}", productId))
                .andExpect(status().isOk())
                .andExpect(view().name("product-data"));

        verify(productService).getProductById(productId);
    }

    @Test
    void testCreateProduct() throws Exception {
        var userDto = mock(UserResponseDTO.class);
        when(userDto.userId()).thenReturn(UUID.randomUUID());
        when(userService.findByUsername("admin")).thenReturn(userDto);
        when(principal.getName()).thenReturn("admin");
        mockMvc.perform(get("/products/new").principal(principal))  // Передаем principal
                .andExpect(status().isOk())
                .andExpect(view().name("add-product"));

        verify(categoryService, times(2)).findAll();
    }

    @Test
    void testSaveProduct_withValidationErrors() throws Exception {
        var userDto = mock(UserResponseDTO.class);
        when(userDto.userId()).thenReturn(UUID.randomUUID());
        when(userService.findByUsername("admin")).thenReturn(userDto);
        when(principal.getName()).thenReturn("admin");
        Product product = new Product();

        mockMvc.perform(multipart("/products")
                        .file((MockMultipartFile) picture)
                        .param("imageOrder", "1")
                        .principal(principal)
                        .flashAttr("product", product))
                .andExpect(status().isOk())
                .andExpect(view().name("add-product"));

        verify(productService, never()).createProduct(any(), any(), any());
    }

    @Test
    void testSaveProduct_withSuccess() throws Exception {
        when(principal.getName()).thenReturn("admin");
        String imageOrder = "1";

        mockMvc.perform(multipart("/products")
                        .file((MockMultipartFile) picture)
                        .param("imageOrder", imageOrder)
                        .principal(principal)
                        .flashAttr("product", product))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/products"));

        verify(productService).createProduct(any(), any(), eq(imageOrder));
    }

    @Test
    void testSaveProduct_withProductCreationException() throws Exception {
        var userDto = mock(UserResponseDTO.class);
        when(userDto.userId()).thenReturn(UUID.randomUUID());
        when(userService.findByUsername("admin")).thenReturn(userDto);
        when(principal.getName()).thenReturn("admin");
        String imageOrder = "1";

        doThrow(new ProductCreationException("Product creation failed")).when(productService).createProduct(any(), any(), eq(imageOrder));

        mockMvc.perform(multipart("/products")
                        .file((MockMultipartFile) picture)
                        .param("imageOrder", imageOrder)
                        .param("pictures", "image.jpg")
                        .principal(principal)
                        .flashAttr("product", product))
                .andExpect(status().isOk())
                .andExpect(view().name("add-product"));

        verify(productService).createProduct(any(), any(), eq(imageOrder));
    }

    @Test
    void testDeleteProduct() throws Exception {
        UUID productId = UUID.randomUUID();

        mockMvc.perform(post("/products/delete/{productId}", productId))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/products"));

        verify(productService).deleteProduct(productId);
    }

    @Test
    void testDeleteProduct_withDeleteProductException() throws Exception {
        UUID productId = UUID.randomUUID();
        doThrow(new DeleteProductException("Error deleting product")).when(productService).deleteProduct(productId);

        mockMvc.perform(post("/products/delete/{productId}", productId))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/products"));

        verify(productService).deleteProduct(productId);
    }

    @Test
    void testDeleteImage() throws Exception {
        String imageFilename = "image.jpg";
        String productId = UUID.randomUUID().toString();

        mockMvc.perform(post("/products/delete-image")
                        .param("imageFilename", imageFilename)
                        .param("productId", productId))
                .andExpect(status().isOk());

        verify(productService).removeImageFromProduct(UUID.fromString(productId), imageFilename);
    }
}
