package com.oa7.dao;

import com.oa7.pojo.Notification;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface NotificationDao {

    @Insert("INSERT INTO day.notification(type, title, content, target_number, biz_id) " +
            "VALUES(#{type}, #{title}, #{content}, #{targetNumber}, #{bizId})")
    int insert(@Param("type") String type, @Param("title") String title,
               @Param("content") String content, @Param("targetNumber") int targetNumber,
               @Param("bizId") String bizId);

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

    /** 根据 biz_id 将所有管理员的通知标记为已读（任一管理员处理后调用） */
    @Update("UPDATE day.notification SET is_read=1 WHERE biz_id=#{bizId} AND type IN ('leave_submitted', 'retroactive_submitted')")
    int markAllReadByBizId(@Param("bizId") String bizId);
}
