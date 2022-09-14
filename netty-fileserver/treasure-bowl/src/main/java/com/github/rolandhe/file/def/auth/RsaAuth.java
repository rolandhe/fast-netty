package com.github.rolandhe.file.def.auth;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class RsaAuth {
    private String bizLine;
    private String url;
    private LocalDateTime expiredAt;
    private Long userId;
}
