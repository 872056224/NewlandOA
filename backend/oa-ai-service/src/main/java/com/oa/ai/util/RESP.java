package com.oa.ai.util;

import lombok.Data;

/**
 * 统一返回体（与 OA-2 / OA-7 服务的 RESP 结构保持一致，便于前端复用解析逻辑）
 */
@Data
public class RESP {

    private Object data;
    private int code = 200;
    private String message;

    public RESP(Object data) {
        this.data = data;
    }

    public RESP(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public static RESP ok(Object data) {
        return new RESP(data);
    }

    public static RESP error(String message) {
        return new RESP(500, message);
    }

    public static RESP error(int code, String message) {
        return new RESP(code, message);
    }
}
