package com.oa2.dao;

import com.oa2.pojo.Notification;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface NotificationDao {

    @Insert("INSERT INTO day.notification(type, title, content, target_number, biz_id) " +
            "VALUES(#{type}, #{title}, #{content}, #{target_number}, #{biz_id})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Notification notification);

    @Select("SELECT * FROM day.notification WHERE target_number=#{number} ORDER BY create_time DESC LIMIT #{offset}, #{limit}")
    List<Notification> selectByTarget(@Param("number") int number, @Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT count(*) FROM day.notification WHERE target_number=#{number}")
    int countByTarget(@Param("number") int number);

    @Select("SELECT count(*) FROM day.notification WHERE target_number=#{number} AND is_read=0")
    int countUnreadByTarget(@Param("number") int number);

    @Update("UPDATE day.notification SET is_read=1 WHERE id=#{id} AND target_number=#{number}")
    int markAsRead(@Param("id") int id, @Param("number") int number);

    @Update("UPDATE day.notification SET is_read=1 WHERE target_number=#{number} AND is_read=0")
    int markAllAsRead(@Param("number") int number);
}
