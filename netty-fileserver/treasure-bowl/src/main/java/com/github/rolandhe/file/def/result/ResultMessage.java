package com.github.rolandhe.file.def.result;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResultMessage {
    private int code;
    private String message;
    private Object data;
}
