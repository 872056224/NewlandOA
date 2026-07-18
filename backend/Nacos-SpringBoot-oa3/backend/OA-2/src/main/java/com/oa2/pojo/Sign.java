package com.oa2.pojo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
@Document(indexName = "employee_sign_records", createIndex = true)
public class Sign {
    @Id
    private String id;
    
    @Field(type = FieldType.Text)
    private String signDate;
    
    @Field(type = FieldType.Integer)
    private int number;
    
    @Field(type = FieldType.Keyword)
    private String state;
    
    @Field(type = FieldType.Keyword)
    private String dept_name;
    
    @Field(type = FieldType.Text)
    private String name;
    
    @Field(type = FieldType.Keyword)
    private String type;
    
    @Field(type = FieldType.Text)
    private String sign_address;
    
    @Field(type = FieldType.Integer)
    private int tag;
    
    // 添加时间戳字段，用于排序和统计
    @Field(type = FieldType.Long)
    private Long timestamp;
    
    // 添加日期字段，用于日期范围查询
    @Field(type = FieldType.Keyword)
    private String dateOnly;
}
