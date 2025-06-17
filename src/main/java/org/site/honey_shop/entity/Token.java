package org.site.honey_shop.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "tokens")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Token {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "token_id")
    private UUID tokenId;

    @Column(name = "username")
    private String username;

    @Column(name = "access_token")
    private String accessToken;

    @Column(name = "is_access_token_valid")
    private boolean isAccessTokenValid;

    @Column(name = "refresh_token")
    private String refreshToken;

    @Column(name = "is_refresh_token_valid")
    private boolean isRefreshTokenValid;

    @Column(name = "create_date")
    @CreationTimestamp
    private LocalDateTime createDate;

    @Column(name = "update_date")
    @UpdateTimestamp
    private LocalDateTime updateDate;
}