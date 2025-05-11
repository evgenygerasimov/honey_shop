package org.site.honey_shop.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.site.honey_shop.service.CdekCacheService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

@RestController
@RequestMapping("/cdek")
@AllArgsConstructor
public class CdekController {

    private final CdekCacheService cdekCacheService;

    @GetMapping("/offices")
    public ResponseEntity<String> getOffices(@RequestParam Map<String, String> params) {
        String json = cdekCacheService.getOfficesWithCaching(params);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(json);
    }

    @PostMapping("/offices")
    public ResponseEntity<byte[]> proxyPostToService(@RequestBody(required = false) byte[] body,
                                                     HttpServletRequest request) throws IOException {
        URL url = new URL("http://lamp-server/service.php");

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", request.getContentType());

        if (body != null && body.length > 0) {
            try (OutputStream os = connection.getOutputStream()) {
                os.write(body);
            }
        }

        int responseCode = connection.getResponseCode();
        InputStream inputStream = (responseCode >= 200 && responseCode < 400)
                ? connection.getInputStream() : connection.getErrorStream();
        byte[] responseBody = inputStream.readAllBytes();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf(connection.getContentType()));
        return new ResponseEntity<>(responseBody, headers, HttpStatus.valueOf(responseCode));
    }
}
