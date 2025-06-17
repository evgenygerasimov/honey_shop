package org.site.honey_shop.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.site.honey_shop.dto.UserResponseDTO;
import org.site.honey_shop.entity.Category;
import org.site.honey_shop.entity.Product;
import org.site.honey_shop.repository.ProductRepository;
import org.site.honey_shop.service.ShowcaseService;
import org.site.honey_shop.service.UserService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.util.*;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
class ShowcaseAdminControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ShowcaseService showcaseService;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private ShowcaseAdminController showcaseAdminController;

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

        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/views/");
        viewResolver.setSuffix(".jsp");

        mockMvc = MockMvcBuilders
                .standaloneSetup(showcaseAdminController)
                .setViewResolvers(viewResolver)
                .build();
    }

    @Test
    void testShowShowcaseManagementPage() throws Exception {
        Category category1 = new Category();
        category1.setCategoryId(UUID.randomUUID());
        category1.setName("Category 1");

        Category category2 = new Category();
        category2.setCategoryId(UUID.randomUUID());
        category2.setName("Category 2");

        List<Category> categories = List.of(category1, category2);
        when(showcaseService.getShowcase()).thenReturn(categories);

        List<Product> products1 = List.of(new Product());
        List<Product> products2 = List.of(new Product(), new Product());

        when(productRepository.findByCategory_CategoryIdOrderByShowcaseOrderAsc(category1.getCategoryId()))
                .thenReturn(products1);
        when(productRepository.findByCategory_CategoryIdOrderByShowcaseOrderAsc(category2.getCategoryId()))
                .thenReturn(products2);

        mockMvc.perform(get("/showcase"))
                .andExpect(status().isOk())
                .andExpect(view().name("showcase"))
                .andExpect(model().attributeExists("authUserId"))
                .andExpect(model().attributeExists("categoryProductsMap"));
    }

    @Test
    void testReorderShowcase() throws Exception {
        UUID category1Id = UUID.randomUUID();
        UUID category2Id = UUID.randomUUID();

        UUID product1Id = UUID.randomUUID();
        UUID product2Id = UUID.randomUUID();

        String jsonPayload = """
                {
                    "categoryOrder": ["%s", "%s"],
                    "productOrder": {
                        "%s": ["%s"],
                        "%s": ["%s"]
                    }
                }
                """.formatted(
                category1Id, category2Id,
                category1Id, product1Id,
                category2Id, product2Id
        );

        mockMvc.perform(post("/showcase/reorder")
                        .contentType("application/json")
                        .content(jsonPayload))
                .andExpect(status().isOk());

        verify(showcaseService).reorder(
                eq(List.of(category1Id, category2Id)),
                eq(Map.of(
                        category1Id, List.of(product1Id),
                        category2Id, List.of(product2Id)
                ))
        );
    }
}
