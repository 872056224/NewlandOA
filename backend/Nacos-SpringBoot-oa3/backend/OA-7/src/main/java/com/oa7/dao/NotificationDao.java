package com.oa7.dao;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface NotificationDao {

    @Insert("INSERT INTO day.notification(type, title, content, target_number, biz_id) " +
            "VALUES(#{type}, #{title}, #{content}, #{targetNumber}, #{bizId})")
    int insert(@Param("type") String type, @Param("title") String title,
               @Param("content") String content, @Param("targetNumber") int targetNumber,
               @Param("bizId") String bizId);
}
