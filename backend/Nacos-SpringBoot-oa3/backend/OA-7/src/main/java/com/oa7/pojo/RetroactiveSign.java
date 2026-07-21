package com.oa7.pojo;

import lombok.Data;

@Data
public class RetroactiveSign {
    private int id;
    private int number;
    private String sign_date;
    private String type;
    private String reason;
    private String status;
    private String create_time;
}
