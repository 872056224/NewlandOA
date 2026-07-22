package com.oa7.dao;

import com.oa7.pojo.Leave;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface LeaveDao {

    @Select("SELECT * FROM day.leave WHERE status='待审批' ORDER BY start_date DESC")
    List<Leave> selectPending();

    @Select("SELECT * FROM day.leave ORDER BY start_date DESC")
    List<Leave> selectAll();

    @Select("SELECT * FROM day.leave WHERE status=#{status} ORDER BY start_date DESC")
    List<Leave> selectByStatus(@Param("status") String status);

    @Select("SELECT * FROM day.leave WHERE id=#{id}")
    Leave selectById(@Param("id") String id);

    @Update("UPDATE day.leave SET status=#{status} WHERE id=#{id}")
    int updateStatus(@Param("id") String id, @Param("status") String status);

    /** 带乐观锁的状态更新（version 防并发） */
    @Update("UPDATE day.leave SET status=#{status}, version=version+1 WHERE id=#{id} AND version=#{version}")
    int updateStatusWithVersion(@Param("id") String id, @Param("status") String status, @Param("version") int version);

    @Select("SELECT count(*) FROM day.leave WHERE number=#{number} AND status='已批准' " +
            "AND DATE(start_date) <= #{today} AND DATE(end_date) >= #{today}")
    int countApprovedLeaveToday(@Param("number") int number, @Param("today") String today);

    /** 统计某天所有已批准的请假人数 */
    @Select("SELECT COUNT(DISTINCT number) FROM day.leave WHERE status='已批准' " +
            "AND DATE(start_date) <= #{date} AND DATE(end_date) >= #{date}")
    int countApprovedLeaveByDate(@Param("date") String date);
}
