package com.oa7.dao;

import com.oa7.pojo.OvertimeRequest;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Mapper
@Repository
public interface OvertimeDao {

    @Select("SELECT o.id, o.emp_id AS empId, o.overtime_date AS overtimeDate, " +
            "o.start_time AS startTime, o.end_time AS endTime, " +
            "o.total_hours AS totalHours, o.actual_hours AS actualHours, " +
            "o.reason, o.status, o.reject_reason AS rejectReason, " +
            "o.version, o.created_at AS createdAt, o.updated_at AS updatedAt, " +
            "e.name AS empName, d.dept_name AS deptName " +
            "FROM day.overtime_request o " +
            "LEFT JOIN day.emp e ON e.number = o.emp_id " +
            "LEFT JOIN day.department d ON d.dept_id = e.dept_id " +
            "WHERE o.status = 'PENDING' ORDER BY o.created_at DESC")
    List<OvertimeRequest> selectPending();

    @Select("SELECT o.id, o.emp_id AS empId, o.overtime_date AS overtimeDate, " +
            "o.start_time AS startTime, o.end_time AS endTime, " +
            "o.total_hours AS totalHours, o.actual_hours AS actualHours, " +
            "o.reason, o.status, o.reject_reason AS rejectReason, " +
            "o.version, o.created_at AS createdAt, o.updated_at AS updatedAt, " +
            "e.name AS empName, d.dept_name AS deptName " +
            "FROM day.overtime_request o " +
            "LEFT JOIN day.emp e ON e.number = o.emp_id " +
            "LEFT JOIN day.department d ON d.dept_id = e.dept_id " +
            "WHERE o.status = 'PENDING' AND e.dept_id = #{deptId} ORDER BY o.created_at DESC")
    List<OvertimeRequest> selectPendingByDept(@Param("deptId") int deptId);

    @Select("SELECT o.id, o.emp_id AS empId, o.overtime_date AS overtimeDate, " +
            "o.start_time AS startTime, o.end_time AS endTime, " +
            "o.total_hours AS totalHours, o.actual_hours AS actualHours, " +
            "o.reason, o.status, o.reject_reason AS rejectReason, " +
            "o.version, o.created_at AS createdAt, o.updated_at AS updatedAt, " +
            "e.name AS empName, d.dept_name AS deptName " +
            "FROM day.overtime_request o " +
            "LEFT JOIN day.emp e ON e.number = o.emp_id " +
            "LEFT JOIN day.department d ON d.dept_id = e.dept_id " +
            "WHERE o.id = #{id}")
    OvertimeRequest selectById(@Param("id") int id);

    @Update("UPDATE day.overtime_request SET status=#{status}, actual_hours=#{actualHours}, " +
            "reject_reason=#{rejectReason}, version=version+1 " +
            "WHERE id=#{id} AND version=#{version}")
    int updateStatusWithVersion(OvertimeRequest req);

    /** 查询某员工某月的加班总时长 */
    @Select("SELECT COALESCE(SUM(actual_hours),0) FROM day.overtime_request " +
            "WHERE emp_id=#{empId} AND status='APPROVED' " +
            "AND overtime_date BETWEEN #{start} AND #{end}")
    java.math.BigDecimal sumMonthlyHours(@Param("empId") int empId,
                                          @Param("start") LocalDate start,
                                          @Param("end") LocalDate end);
}
