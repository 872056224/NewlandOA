package com.oa2.pojo;

import lombok.Data;

@Data
public class Leave {
    private String id;
    private int number;
    private String name;
    private String type;
    private String dept_name;
    private String start_date;
    private String end_date;
    private String reason;
    private String status;
}
