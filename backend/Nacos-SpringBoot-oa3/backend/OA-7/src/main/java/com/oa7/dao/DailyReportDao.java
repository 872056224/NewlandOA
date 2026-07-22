package com.oa7.dao;

import com.oa7.pojo.DailyReport;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * 日报统计数据访问层 - day.daily_report 表
 */
@Repository
@Mapper
public interface DailyReportDao {

    /**
     * 插入或更新日报统计（存在则更新）
     */
    @Insert("INSERT INTO day.daily_report(" +
            "report_date, total_employees, normal_count, late_count, early_count, " +
            "late_early_count, leave_count, absence_count, missing_card_count, holiday_count, attendance_rate" +
            ") VALUES(" +
            "#{reportDate}, #{totalEmployees}, #{normalCount}, #{lateCount}, #{earlyCount}, " +
            "#{lateEarlyCount}, #{leaveCount}, #{absenceCount}, #{missingCardCount}, #{holidayCount}, #{attendanceRate}" +
            ") ON DUPLICATE KEY UPDATE " +
            "total_employees=#{totalEmployees}, normal_count=#{normalCount}, late_count=#{lateCount}, " +
            "early_count=#{earlyCount}, late_early_count=#{lateEarlyCount}, leave_count=#{leaveCount}, " +
            "absence_count=#{absenceCount}, missing_card_count=#{missingCardCount}, holiday_count=#{holidayCount}, " +
            "attendance_rate=#{attendanceRate}")
    int insertOrUpdate(DailyReport report);

    /**
     * 根据日期查询日报统计
     */
    @Select("SELECT * FROM day.daily_report WHERE report_date = #{date}")
    @Results(id = "dailyReportResult", value = {
        @Result(property = "id", column = "id"),
        @Result(property = "reportDate", column = "report_date"),
        @Result(property = "totalEmployees", column = "total_employees"),
        @Result(property = "normalCount", column = "normal_count"),
        @Result(property = "lateCount", column = "late_count"),
        @Result(property = "earlyCount", column = "early_count"),
        @Result(property = "lateEarlyCount", column = "late_early_count"),
        @Result(property = "leaveCount", column = "leave_count"),
        @Result(property = "absenceCount", column = "absence_count"),
        @Result(property = "missingCardCount", column = "missing_card_count"),
        @Result(property = "holidayCount", column = "holiday_count"),
        @Result(property = "attendanceRate", column = "attendance_rate"),
        @Result(property = "createdAt", column = "created_at")
    })
    DailyReport selectByDate(@Param("date") LocalDate date);

    /**
     * 查询指定日期范围内的日报统计
     */
    @Select("SELECT * FROM day.daily_report WHERE report_date BETWEEN #{start} AND #{end} ORDER BY report_date")
    @ResultMap("dailyReportResult")
    List<DailyReport> selectByDateRange(@Param("start") LocalDate start, @Param("end") LocalDate end);
}
