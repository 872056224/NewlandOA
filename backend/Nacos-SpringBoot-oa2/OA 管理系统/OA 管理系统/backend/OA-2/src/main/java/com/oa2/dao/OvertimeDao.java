package com.oa2.dao;

import com.oa2.pojo.OvertimeRequest;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Mapper
@Repository
public interface OvertimeDao {

    @Insert("INSERT INTO day.overtime_request(emp_id, overtime_date, start_time, end_time, total_hours, reason, status) " +
            "VALUES(#{empId}, #{overtimeDate}, #{startTime}, #{endTime}, #{totalHours}, #{reason}, 'PENDING')")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(OvertimeRequest req);

    @Select("SELECT id, emp_id AS empId, overtime_date AS overtimeDate, " +
            "start_time AS startTime, end_time AS endTime, " +
            "total_hours AS totalHours, actual_hours AS actualHours, " +
            "reason, status, reject_reason AS rejectReason, " +
            "version, created_at AS createdAt, updated_at AS updatedAt " +
            "FROM day.overtime_request WHERE emp_id=#{empId} " +
            "ORDER BY created_at DESC LIMIT #{offset}, #{limit}")
    List<OvertimeRequest> selectByEmp(@Param("empId") int empId, @Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM day.overtime_request WHERE emp_id=#{empId}")
    int countByEmp(@Param("empId") int empId);

    @Select("SELECT COALESCE(SUM(actual_hours),0) FROM day.overtime_request " +
            "WHERE emp_id=#{empId} AND status='APPROVED' " +
            "AND overtime_date BETWEEN #{start} AND #{end}")
    Double sumMonthlyHours(@Param("empId") int empId,
                           @Param("start") LocalDate start,
                           @Param("end") LocalDate end);
}
