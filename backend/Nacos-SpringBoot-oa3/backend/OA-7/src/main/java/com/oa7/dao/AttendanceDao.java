package com.oa7.dao;

import com.oa7.config.MyBatisEnumTypeHandler;
import com.oa7.constant.AttendanceStatus;
import com.oa7.constant.TodayStatus;
import com.oa7.pojo.Attendance;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 考勤记录数据访问层 - day.attendance 表
 */
@Repository
@Mapper
public interface AttendanceDao {

    /**
     * 根据日期查询所有考勤记录
     */
    @Select("SELECT * FROM day.attendance WHERE date = #{date}")
    @Results(id = "attendanceResult", value = {
        @Result(property = "id", column = "id"),
        @Result(property = "empId", column = "emp_id"),
        @Result(property = "date", column = "date"),
        @Result(property = "checkInTime", column = "check_in_time"),
        @Result(property = "checkOutTime", column = "check_out_time"),
        @Result(property = "todayStatus", column = "today_status",
                typeHandler = MyBatisEnumTypeHandler.class),
        @Result(property = "attendanceStatus", column = "attendance_status",
                typeHandler = MyBatisEnumTypeHandler.class),
        @Result(property = "remark", column = "remark"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "updatedAt", column = "updated_at")
    })
    List<Attendance> selectByDate(@Param("date") LocalDate date);

    /**
     * 统计某天的考勤总人数
     */
    @Select("SELECT COUNT(*) FROM day.attendance WHERE date = #{date}")
    int countByDate(@Param("date") LocalDate date);

    /**
     * 统计某天已签到/签退的人数 (today_status IN 'CHECKED_IN', 'CHECKED_OUT')
     */
    @Select("SELECT COUNT(*) FROM day.attendance WHERE date = #{date} AND today_status IN ('CHECKED_IN', 'CHECKED_OUT')")
    int countCheckedInByDate(@Param("date") LocalDate date);

    /**
     * 统计某天迟到人数 (attendance_status = 'LATE')
     */
    @Select("SELECT COUNT(*) FROM day.attendance WHERE date = #{date} AND attendance_status = 'LATE'")
    int countLateByDate(@Param("date") LocalDate date);

    /**
     * 统计某天请假人数 (today_status = 'LEAVE')
     */
    @Select("SELECT COUNT(*) FROM day.attendance WHERE date = #{date} AND today_status = 'LEAVE'")
    int countLeaveByDate(@Param("date") LocalDate date);

    /**
     * 统计某天缺勤人数 (today_status = 'NOT_CHECKED_IN')
     */
    @Select("SELECT COUNT(*) FROM day.attendance WHERE date = #{date} AND today_status = 'NOT_CHECKED_IN'")
    int countAbsenceByDate(@Param("date") LocalDate date);

    // ===== Task 5: Today's Real-time Statistics =====

    /**
     * 统计某天未签到人数
     */
    @Select("SELECT COUNT(*) FROM day.attendance WHERE date=#{date} AND today_status='NOT_CHECKED_IN'")
    int countNotCheckedInByDate(LocalDate date);

    /**
     * 按今日状态统计人数
     */
    @Select("SELECT COUNT(*) FROM day.attendance WHERE date=#{date} AND today_status=#{status}")
    int countByTodayStatus(@Param("date") LocalDate date, @Param("status") String status);

    // ===== Task 6: Yesterday's Statistics =====

    /**
     * 按考勤结算状态分组统计
     */
    @Select("SELECT attendance_status, COUNT(*) as cnt FROM day.attendance WHERE date=#{date} GROUP BY attendance_status")
    List<Map<String, Object>> countGroupByStatus(LocalDate date);

    /**
     * 根据员工ID和日期查询考勤记录
     */
    @Select("SELECT * FROM day.attendance WHERE emp_id = #{empId} AND date = #{date}")
    @ResultMap("attendanceResult")
    Attendance selectByEmpAndDate(@Param("empId") Integer empId, @Param("date") LocalDate date);

    /**
     * 更新考勤结算状态
     */
    @Update("UPDATE day.attendance SET attendance_status = #{attendanceStatus} WHERE id = #{id}")
    int updateAttendanceStatus(@Param("id") Long id, @Param("attendanceStatus") AttendanceStatus attendanceStatus);

    /**
     * 更新实时状态 (today_status) 通过员工ID和日期
     */
    @Update("UPDATE day.attendance SET today_status=#{todayStatus} WHERE emp_id=#{empId} AND date=#{date}")
    int updateTodayStatusByEmpAndDate(@Param("empId") int empId, @Param("date") LocalDate date,
                                       @Param("todayStatus") TodayStatus todayStatus);

    /**
     * 更新签到/签退时间
     */
    @Update("UPDATE day.attendance SET check_in_time=#{checkInTime}, check_out_time=#{checkOutTime} " +
            "WHERE emp_id=#{empId} AND date=#{date}")
    int updateCheckTime(Attendance attendance);

    /**
     * 单条插入或更新（ON DUPLICATE KEY）
     */
    @Insert("INSERT INTO day.attendance(emp_id, date, today_status) VALUES(#{empId}, #{date}, #{todayStatus}) " +
            "ON DUPLICATE KEY UPDATE today_status=#{todayStatus}")
    int insertOrUpdate(@Param("empId") int empId, @Param("date") LocalDate date,
                       @Param("todayStatus") TodayStatus todayStatus);

    /**
     * 更新缺时时长
     */
    @Update("UPDATE day.attendance SET missing_duration=#{minutes} WHERE id=#{id}")
    int updateMissingDuration(@Param("id") Long id, @Param("minutes") int minutes);

    /**
     * 批量插入或更新（用于任务4的午夜自动创建）
     */
    @Insert({"<script>",
            "INSERT INTO day.attendance(emp_id, date, today_status) VALUES ",
            "<foreach collection='list' item='item' separator=','>",
            "(#{item.empId}, #{item.date}, #{item.todayStatus})",
            "</foreach>",
            "ON DUPLICATE KEY UPDATE today_status=VALUES(today_status)",
            "</script>"})
    int batchInsertOrUpdate(@Param("list") List<Attendance> list);

    // ===== Monthly Report: Employee attendance within date range =====

    /**
     * 查询指定员工在指定日期范围内的考勤记录
     */
    @Select("SELECT * FROM day.attendance WHERE emp_id = #{empId} AND date BETWEEN #{start} AND #{end} ORDER BY date")
    @ResultMap("attendanceResult")
    List<Attendance> selectByEmpAndDateRange(@Param("empId") int empId,
                                              @Param("start") LocalDate start,
                                              @Param("end") LocalDate end);
}
