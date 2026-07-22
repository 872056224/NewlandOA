package com.oa7.pojo;

import lombok.Data;

@Data
public class Notification {
    private int id;
    private String type;
    private String title;
    private String content;
    private int target_number;
    private String biz_id;
    private int is_read;
    private String create_time;
}
