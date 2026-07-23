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

    /** 按申请人部门ID查询待审批（通过JOIN emp表，比dept_name字符串对比更可靠） */
    @Select("SELECT l.* FROM day.leave l " +
            "INNER JOIN day.emp e ON e.number = l.number " +
            "WHERE l.status='待审批' AND e.dept_id = #{deptId} " +
            "ORDER BY l.start_date DESC")
    List<Leave> selectPendingByEmpDept(@Param("deptId") int deptId);

    /** 按申请人部门ID和状态查询 */
    @Select("SELECT l.* FROM day.leave l " +
            "INNER JOIN day.emp e ON e.number = l.number " +
            "WHERE l.status=#{status} AND e.dept_id = #{deptId} " +
            "ORDER BY l.start_date DESC")
    List<Leave> selectByStatusByEmpDept(@Param("status") String status, @Param("deptId") int deptId);

    /** 根据请假单ID查询申请人所属部门 */
    @Select("SELECT e.dept_id FROM day.emp e " +
            "INNER JOIN day.leave l ON l.number = e.number " +
            "WHERE l.id = #{id}")
    Integer selectDeptIdByLeaveId(@Param("id") String id);
}
