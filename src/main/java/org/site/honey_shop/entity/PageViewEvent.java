package org.site.honey_shop.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class PageViewEvent {

    private String path;
    private String ip;
    private String userAgent;
    private String sessionId;
    private LocalDateTime visitTime;
}
