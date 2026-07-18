package com.oa2.util;

import lombok.Data;

/**
 * @name: chenle
 * @Date: 2021/11/30 22:08
 * @Author: IAO
 * @Description: 返回数据的封装类
 */
@Data
public class RESP {

    private Object data;
    private Object data1;
    private Object data2;
    private Object data3;
    private int pageNum;
    private int total;
    private int code = 200; // 状态码，默认成功
    private String message; // 消息内容

    public RESP(Object data , Object data1 , Object data2 , Object data3) {
        this.data = data;
        this.data1 = data1;
        this.data2 = data2;
        this.data3 = data3;
    }

    public RESP(Object data , int pageNum , int total) {
        this.data = data;
        this.pageNum = pageNum;
        this.total = total;
    }

    public RESP(Object data) {
        this.data = data;
    }

    public RESP(Object data , Object data1) {
        this.data = data;
        this.data1 = data1;
    }

    public RESP(Object data , Object data1 , Object data2) {
        this.data = data;
        this.data1 = data1;
        this.data2 = data2;
    }

    // 错误响应构造函数
    public RESP(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public static RESP ok(Object data , Object data1 , Object data2) {
        return new RESP(data , data1 , data2);
    }

    public static RESP ok(Object data , Object data1 , Object data2 , Object data3) {
        return new RESP(data , data1 , data2 , data3);
    }

    public static RESP ok(Object data , int pageNum , int total) {
        return new RESP(data , pageNum , total);
    }

    public static RESP ok(Object data , Object data1) {
        return new RESP(data , data1);
    }

    public static RESP ok(Object data) {
        return new RESP(data);
    }

    // 添加错误方法
    public static RESP error(String message) {
        return new RESP(500, message);
    }

    public static RESP error(int code, String message) {
        return new RESP(code, message);
    }
}
