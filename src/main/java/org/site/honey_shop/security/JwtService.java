package org.site.honey_shop.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.site.honey_shop.constants.TokenLifeTime;
import org.site.honey_shop.entity.Token;
import org.site.honey_shop.repository.TokenRepository;
import org.site.honey_shop.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtService {

    @Value("${myapp.secret.key}")
    private String secretKey;
    private final long validityFifteenMinutes = TokenLifeTime.ACCESS_TOKEN.toMillis();
    private final long validitySevenDays = TokenLifeTime.REFRESH_TOKEN.toMillis();
    private final UserService userService;
    private final ApplicationContext context;


    private final TokenRepository tokenRepository;

    public String generateAccessToken(String username) {
        System.out.println("context class inside generateAccessToken: " + context.getClass());
        UserDetailsService userDetailsService = context.getBean(UserDetailsService.class);
        Collection<? extends GrantedAuthority> authorities =
                userDetailsService.loadUserByUsername(username).getAuthorities();

        List<String> roles = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return Jwts.builder()
                .subject(username)
                .claim("userId", userService.findByUsername(username).userId())
                .claim("authorities", roles)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + validityFifteenMinutes))
                .setId(UUID.randomUUID().toString())
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();
    }

    public String generateRefreshToken(String username) {
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + validitySevenDays))
                .setId(UUID.randomUUID().toString())
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();
    }

    public String extractUserName(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolvers) {
        final Claims claims = extractAllClaims(token);
        return claimsResolvers.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public void invalidateToken(Token token) {
        if (token != null) {
            token.setAccessTokenValid(false);
            token.setRefreshTokenValid(false);
            tokenRepository.save(token);
        }
    }

    public Token saveToken(String username, String accessToken, String refreshToken) {
        Token token = new Token();
        token.setUsername(username);
        token.setAccessToken(accessToken);
        token.setAccessTokenValid(true);
        token.setRefreshToken(refreshToken);
        token.setRefreshTokenValid(true);
        return tokenRepository.save(token);
    }

    public List<Token> getTokens() {
        return tokenRepository.findAll();
    }

    public Token findByAccessToken(String token) {
        return tokenRepository.findByAccessToken(token)
                .orElseThrow(() -> new RuntimeException("Token not found"));
    }

    public Token findByRefreshToken(String token) {
        return tokenRepository.findByRefreshToken(token)
                .orElseThrow(() -> new RuntimeException("Token not found"));
    }

    public boolean isAccessTokenExpiredAndInvalid(String accessToken) {
        Token accessTokenObj = tokenRepository.findByAccessToken(accessToken)
                .orElseThrow(() -> new RuntimeException("Token not found"));
        LocalDateTime expirationTime = accessTokenObj.getCreateDate()
                .plus(Duration.of(validityFifteenMinutes, ChronoUnit.MILLIS));
        return expirationTime.isBefore(LocalDateTime.now()) || !accessTokenObj.isAccessTokenValid();
    }

    public boolean isRefreshTokenExpiredAndInvalid(String refreshToken) {
        Token refreshTokenObj = tokenRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Token not found"));
        LocalDateTime expirationTime = refreshTokenObj.getCreateDate()
                .plus(Duration.of(validitySevenDays, ChronoUnit.MILLIS));
        return expirationTime.isBefore(LocalDateTime.now()) || !refreshTokenObj.isRefreshTokenValid();
    }
}