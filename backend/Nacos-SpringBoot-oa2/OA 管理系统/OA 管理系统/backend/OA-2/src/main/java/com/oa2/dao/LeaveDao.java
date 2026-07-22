package com.oa2.dao;

import com.oa2.pojo.Leave;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface LeaveDao {

    @Insert("INSERT INTO day.leave(id, number, name, type, dept_name, start_date, end_date, reason, status, duration) " +
            "VALUES(#{id}, #{number}, #{name}, #{type}, #{dept_name}, #{start_date}, #{end_date}, #{reason}, #{status}, #{duration})")
    int insert(Leave leave);

    @Select("SELECT * FROM day.leave WHERE number=#{number} ORDER BY start_date DESC LIMIT #{offset}, #{limit}")
    List<Leave> selectByNumberPage(@Param("number") int number, @Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT count(*) FROM day.leave WHERE number=#{number}")
    int countByNumber(@Param("number") int number);

    @Select("SELECT count(*) FROM day.leave WHERE number=#{number} AND status='已批准' " +
            "AND DATE(start_date) <= #{today} AND DATE(end_date) >= #{today}")
    int countApprovedLeaveToday(@Param("number") int number, @Param("today") String today);
}
