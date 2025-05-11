package org.site.honey_shop.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.site.honey_shop.service.CdekCacheService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CdekControllerTest {

    @Mock
    private CdekCacheService cdekCacheService;

    @InjectMocks
    private CdekController cdekController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(cdekController).build();
    }

    @Test
    void testGetOffices() throws Exception {
        String mockResponse = "{\"offices\": [{\"id\": 1, \"name\": \"Office 1\"}]}";

        // Мокаем кэшированные данные
        when(cdekCacheService.getOfficesWithCaching(anyMap())).thenReturn(mockResponse);

        mockMvc.perform(get("/cdek/offices")
                        .param("city", "Moscow"))
                .andExpect(status().isOk())  // Проверяем, что статус 200 OK
                .andExpect(content().json(mockResponse));  // Проверяем, что возвращается нужный JSON
    }
}