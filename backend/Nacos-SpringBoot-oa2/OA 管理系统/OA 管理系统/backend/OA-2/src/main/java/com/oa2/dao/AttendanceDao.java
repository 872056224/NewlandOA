package com.oa2.dao;

import com.oa2.config.MyBatisEnumTypeHandler;
import com.oa2.constant.AttendanceStatus;
import com.oa2.constant.TodayStatus;
import com.oa2.pojo.Attendance;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
@Repository
public interface AttendanceDao {

    @Results(id = "attendanceMap", value = {
        @Result(property = "id", column = "id"),
        @Result(property = "empId", column = "emp_id"),
        @Result(property = "date", column = "date"),
        @Result(property = "checkInTime", column = "check_in_time"),
        @Result(property = "checkOutTime", column = "check_out_time"),
        @Result(property = "todayStatus", column = "today_status",
                typeHandler = MyBatisEnumTypeHandler.class),
        @Result(property = "attendanceStatus", column = "attendance_status",
                typeHandler = MyBatisEnumTypeHandler.class),
        @Result(property = "checkInAddress", column = "check_in_address"),
        @Result(property = "checkOutAddress", column = "check_out_address"),
        @Result(property = "remark", column = "remark"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "updatedAt", column = "updated_at")
    })
    @Select("SELECT * FROM day.attendance WHERE emp_id=#{empId} AND date=#{date}")
    Attendance selectByEmpAndDate(@Param("empId") int empId, @Param("date") LocalDate date);

    @ResultMap("attendanceMap")
    @Select("SELECT * FROM day.attendance WHERE emp_id=#{empId} ORDER BY date DESC LIMIT #{offset}, #{limit}")
    List<Attendance> selectByEmpPage(@Param("empId") int empId, @Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT count(*) FROM day.attendance WHERE emp_id=#{empId}")
    int countByEmp(@Param("empId") int empId);

    @Insert("INSERT INTO day.attendance(emp_id, date, check_in_time, today_status, check_in_address) " +
            "VALUES(#{empId}, #{date}, #{checkInTime}, 'CHECKED_IN', #{address}) " +
            "ON DUPLICATE KEY UPDATE check_in_time=#{checkInTime}, check_in_address=#{address}, today_status=" +
            "CASE WHEN today_status='CHECKED_OUT' THEN 'CHECKED_OUT' ELSE 'CHECKED_IN' END")
    int checkIn(@Param("empId") int empId, @Param("date") LocalDate date,
                @Param("checkInTime") LocalDateTime checkInTime,
                @Param("address") String address);

    @Update("UPDATE day.attendance SET check_out_time=#{checkOutTime}, check_out_address=#{address}, today_status='CHECKED_OUT' " +
            "WHERE emp_id=#{empId} AND date=#{date}")
    int checkOut(@Param("empId") int empId, @Param("date") LocalDate date,
                 @Param("checkOutTime") LocalDateTime checkOutTime,
                 @Param("address") String address);

    @Update("UPDATE day.attendance SET today_status=#{status} WHERE emp_id=#{empId} AND date=#{date}")
    int updateTodayStatus(@Param("empId") int empId, @Param("date") LocalDate date,
                          @Param("status") TodayStatus status);

    @Update("UPDATE day.attendance SET attendance_status=#{status} WHERE emp_id=#{empId} AND date=#{date}")
    int updateAttendanceStatus(@Param("empId") int empId, @Param("date") LocalDate date,
                               @Param("status") AttendanceStatus status);

    @ResultMap("attendanceMap")
    @Select("SELECT * FROM day.attendance WHERE date=#{date}")
    List<Attendance> selectByDate(@Param("date") LocalDate date);

    @Select("SELECT COUNT(*) FROM day.attendance WHERE date=#{date} AND today_status IN ('CHECKED_IN','CHECKED_OUT')")
    int countCheckedInByDate(@Param("date") LocalDate date);

    @Select("SELECT COUNT(*) FROM day.attendance WHERE date=#{date}")
    int countByDate(@Param("date") LocalDate date);

    @Select("SELECT COUNT(*) FROM day.attendance WHERE date=#{date} AND attendance_status='LATE'")
    int countLateByDate(@Param("date") LocalDate date);

    @Select("SELECT COUNT(*) FROM day.attendance WHERE date=#{date} AND today_status='LEAVE'")
    int countLeaveByDate(@Param("date") LocalDate date);
}
