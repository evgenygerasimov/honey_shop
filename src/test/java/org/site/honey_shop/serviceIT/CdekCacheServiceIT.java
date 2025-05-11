package org.site.honey_shop.serviceIT;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.site.honey_shop.TestContainerConfig;
import org.site.honey_shop.service.CdekCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@SpringBootTest
@ActiveProfiles("test")
class CdekCacheServiceIT extends TestContainerConfig {

    @Autowired
    private CdekCacheService cdekCacheService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private RestTemplate restTemplate;

    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        redisTemplate.getConnectionFactory().getConnection().flushAll();
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    void testGetOfficesWithCaching_whenNotInCache_fetchesAndStores() {
        Map<String, String> params = Map.of("action", "offices", "is_handout", "true", "page", "0");
        String cacheKey = cdekCacheService.buildCacheKey(params);
        String expectedResponse = "{\"offices\":[]}";


        mockServer.expect(requestTo(containsString("action=offices")))
                .andExpect(requestTo(containsString("is_handout=true")))
                .andExpect(requestTo(containsString("page=0")))
                .andRespond(withSuccess(expectedResponse, org.springframework.http.MediaType.APPLICATION_JSON));

        String response = cdekCacheService.getOfficesWithCaching(params);

        assertThat(response).isEqualTo(expectedResponse);

        assertThat(redisTemplate.opsForValue().get(cacheKey)).isEqualTo(expectedResponse);

        assertThat(redisTemplate.opsForList().range(cdekCacheService.getCdekCacheKeys(), 0, -1)).contains(cacheKey);
    }

    @Test
    void testGetOfficesWithCaching_whenInCache_returnsCachedValue() {
        Map<String, String> params = Map.of("action", "offices", "is_handout", "true", "page", "1");
        String cacheKey = cdekCacheService.buildCacheKey(params);
        redisTemplate.opsForValue().set(cacheKey, "{\"cached\":true}");

        String result = cdekCacheService.getOfficesWithCaching(params);

        assertThat(result).isEqualTo("{\"cached\":true}");
    }

    @Test
    void testFetchCdekData_whenResponseInvalid_throwsException() {
        String badResponse = "<b>Fatal error</b>";
        String url = "http://fake-url";

        mockServer.expect(requestTo(url))
                .andRespond(withSuccess(badResponse, org.springframework.http.MediaType.TEXT_HTML));

        assertThatThrownBy(() -> cdekCacheService.fetchCdekData(url))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Error while getting data from CDEK API");
    }

    @Test
    void testBuildCacheKey_sortsAndFormatsParams() {
        Map<String, String> params = Map.of("b", "2", "a", "1");
        String cacheKey = cdekCacheService.buildCacheKey(params);

        assertThat(cacheKey).isEqualTo("cdek::a=1&b=2");
    }

    @Test
    void testUpdateCache_storesValueAndPushesKeyToList() {
        Map<String, String> params = Map.of("action", "offices", "page", "0");
        String expected = "{\"data\":\"ok\"}";
        String cacheKey = cdekCacheService.buildCacheKey(params);

        mockServer.expect(requestTo(cdekCacheService.getBaseUrl() + "?action=offices&page=0"))
                .andRespond(withSuccess(expected, org.springframework.http.MediaType.APPLICATION_JSON));

        String response = cdekCacheService.updateCache(params, cacheKey);

        assertThat(response).isEqualTo(expected);
        assertThat(redisTemplate.opsForValue().get(cacheKey)).isEqualTo(expected);
        assertThat(redisTemplate.opsForList().range(cdekCacheService.getCdekCacheKeys(), 0, -1)).contains(cacheKey);
    }
}
