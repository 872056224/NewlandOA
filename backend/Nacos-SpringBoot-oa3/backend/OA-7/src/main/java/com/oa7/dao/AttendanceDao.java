package com.oa7.dao;

import com.oa7.config.MyBatisEnumTypeHandler;
import com.oa7.constant.AttendanceStatus;
import com.oa7.constant.TodayStatus;
import com.oa7.pojo.Attendance;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

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
}
