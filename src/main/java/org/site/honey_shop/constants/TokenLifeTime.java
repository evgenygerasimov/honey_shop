package org.site.honey_shop.constants;

import lombok.Getter;

import java.time.Duration;

@Getter
public enum TokenLifeTime {

    ACCESS_TOKEN(Duration.ofMinutes(15)),
    REFRESH_TOKEN(Duration.ofDays(7));

    private final Duration duration;

    TokenLifeTime(Duration duration) {
        this.duration = duration;
    }

    public long toMillis() {
        return duration.toMillis();
    }

    public long toSeconds() {
        return duration.getSeconds();
    }
}
