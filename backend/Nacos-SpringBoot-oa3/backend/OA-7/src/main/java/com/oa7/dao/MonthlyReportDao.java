package com.oa7.dao;

import com.oa7.pojo.MonthlyReport;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 月度考勤统计数据访问层 - day.monthly_report 表
 */
@Repository
@Mapper
public interface MonthlyReportDao {

    /**
     * 插入或更新月度统计（存在则更新）
     */
    @Insert("INSERT INTO day.monthly_report(" +
            "year_month, emp_id, emp_name, dept_id, work_days, actual_days, " +
            "late_count, early_count, leave_count, absence_count, missing_card_count, attendance_rate" +
            ") VALUES(" +
            "#{yearMonth}, #{empId}, #{empName}, #{deptId}, #{workDays}, #{actualDays}, " +
            "#{lateCount}, #{earlyCount}, #{leaveCount}, #{absenceCount}, #{missingCardCount}, #{attendanceRate}" +
            ") ON DUPLICATE KEY UPDATE " +
            "emp_name=#{empName}, dept_id=#{deptId}, work_days=#{workDays}, actual_days=#{actualDays}, " +
            "late_count=#{lateCount}, early_count=#{earlyCount}, leave_count=#{leaveCount}, " +
            "absence_count=#{absenceCount}, missing_card_count=#{missingCardCount}, " +
            "attendance_rate=#{attendanceRate}")
    int insertOrUpdate(MonthlyReport report);

    /**
     * 根据员工和月份查询月度统计
     */
    @Select("SELECT * FROM day.monthly_report WHERE emp_id = #{empId} AND year_month = #{yearMonth}")
    @Results(id = "monthlyReportResult", value = {
        @Result(property = "id", column = "id"),
        @Result(property = "yearMonth", column = "year_month"),
        @Result(property = "empId", column = "emp_id"),
        @Result(property = "empName", column = "emp_name"),
        @Result(property = "deptId", column = "dept_id"),
        @Result(property = "workDays", column = "work_days"),
        @Result(property = "actualDays", column = "actual_days"),
        @Result(property = "lateCount", column = "late_count"),
        @Result(property = "earlyCount", column = "early_count"),
        @Result(property = "leaveCount", column = "leave_count"),
        @Result(property = "absenceCount", column = "absence_count"),
        @Result(property = "missingCardCount", column = "missing_card_count"),
        @Result(property = "attendanceRate", column = "attendance_rate"),
        @Result(property = "createdAt", column = "created_at")
    })
    MonthlyReport selectByEmpAndMonth(@Param("empId") int empId, @Param("yearMonth") String yearMonth);

    /**
     * 根据部门和月份查询月度统计列表
     */
    @Select("SELECT * FROM day.monthly_report WHERE dept_id = #{deptId} AND year_month = #{yearMonth}")
    @ResultMap("monthlyReportResult")
    List<MonthlyReport> selectByDeptAndMonth(@Param("deptId") int deptId, @Param("yearMonth") String yearMonth);

    /**
     * 根据月份查询所有月度统计
     */
    @Select("SELECT * FROM day.monthly_report WHERE year_month = #{yearMonth}")
    @ResultMap("monthlyReportResult")
    List<MonthlyReport> selectByMonth(@Param("yearMonth") String yearMonth);
}
