package com.oa7.pojo;

import lombok.Data;
import java.time.LocalDate;

@Data
public class Holiday {
    private LocalDate date;
    private String type;       // WORKDAY / HOLIDAY / REST_DAY
    private String description;
    private Integer year;
}
