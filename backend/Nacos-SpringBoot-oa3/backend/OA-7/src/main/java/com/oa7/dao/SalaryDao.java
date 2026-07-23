package com.oa7.dao;

import com.oa7.pojo.SalaryDetail;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Mapper
@Repository
public interface SalaryDao {

    @Insert("INSERT INTO day.salary_detail(emp_id, `year_month`, base_salary, work_days, " +
            "daily_wage, hourly_wage, actual_attendance_days, total_missing_minutes, " +
            "missing_deduction, overtime_hours, overtime_pay, leave_days, leave_deduction, final_salary) " +
            "VALUES(#{empId}, #{yearMonth}, #{baseSalary}, #{workDays}, " +
            "#{dailyWage}, #{hourlyWage}, #{actualAttendanceDays}, #{totalMissingMinutes}, " +
            "#{missingDeduction}, #{overtimeHours}, #{overtimePay}, #{leaveDays}, #{leaveDeduction}, #{finalSalary})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SalaryDetail detail);

    @Select("SELECT sd.id, sd.emp_id AS empId, sd.`year_month` AS yearMonth, " +
            "sd.base_salary AS baseSalary, sd.work_days AS workDays, " +
            "sd.daily_wage AS dailyWage, sd.hourly_wage AS hourlyWage, " +
            "sd.total_missing_minutes AS totalMissingMinutes, " +
            "sd.missing_deduction AS missingDeduction, " +
            "sd.overtime_hours AS overtimeHours, sd.overtime_pay AS overtimePay, " +
            "sd.leave_days AS leaveDays, sd.leave_deduction AS leaveDeduction, " +
            "sd.actual_attendance_days AS actualAttendanceDays, " +
            "sd.final_salary AS finalSalary, sd.status, " +
            "sd.created_at AS createdAt, sd.updated_at AS updatedAt, " +
            "e.name AS empName, d.dept_name AS deptName, du.duty_name AS dutyName " +
            "FROM day.salary_detail sd " +
            "LEFT JOIN day.emp e ON e.number = sd.emp_id " +
            "LEFT JOIN day.department d ON d.dept_id = e.dept_id " +
            "LEFT JOIN day.duty du ON du.duty_id = e.duty_id " +
            "WHERE sd.`year_month` = #{yearMonth} ORDER BY sd.emp_id")
    List<SalaryDetail> selectByMonth(@Param("yearMonth") String yearMonth);

    @Select("SELECT sd.id, sd.emp_id AS empId, sd.`year_month` AS yearMonth, " +
            "sd.base_salary AS baseSalary, sd.work_days AS workDays, " +
            "sd.daily_wage AS dailyWage, sd.hourly_wage AS hourlyWage, " +
            "sd.total_missing_minutes AS totalMissingMinutes, " +
            "sd.missing_deduction AS missingDeduction, " +
            "sd.overtime_hours AS overtimeHours, sd.overtime_pay AS overtimePay, " +
            "sd.leave_days AS leaveDays, sd.leave_deduction AS leaveDeduction, " +
            "sd.actual_attendance_days AS actualAttendanceDays, " +
            "sd.final_salary AS finalSalary, sd.status, " +
            "sd.created_at AS createdAt, sd.updated_at AS updatedAt, " +
            "e.name AS empName, d.dept_name AS deptName " +
            "FROM day.salary_detail sd " +
            "LEFT JOIN day.emp e ON e.number = sd.emp_id " +
            "LEFT JOIN day.department d ON d.dept_id = e.dept_id " +
            "WHERE sd.emp_id = #{empId} AND sd.`year_month` = #{yearMonth}")
    SalaryDetail selectByEmpAndMonth(@Param("empId") int empId, @Param("yearMonth") String yearMonth);

    @Delete("DELETE FROM day.salary_detail WHERE `year_month` = #{yearMonth}")
    int deleteByMonth(@Param("yearMonth") String yearMonth);

    @Select("SELECT COALESCE(SUM(missing_duration),0) FROM day.attendance " +
            "WHERE emp_id = #{empId} AND date BETWEEN #{start} AND #{end}")
    Integer sumMissingMinutes(@Param("empId") int empId, @Param("start") LocalDate start, @Param("end") LocalDate end);

    /** 实际出勤天数（截止今天，有签到记录且非节假日/休息日） */
    @Select("SELECT COUNT(*) FROM day.attendance " +
            "WHERE emp_id = #{empId} AND date BETWEEN #{start} AND #{end} " +
            "AND check_in_time IS NOT NULL " +
            "AND today_status NOT IN ('HOLIDAY','REST_DAY')")
    Integer countActualAttendance(@Param("empId") int empId,
                                   @Param("start") LocalDate start,
                                   @Param("end") LocalDate end);

    @Select("SELECT COUNT(DISTINCT DATE(start_date)) FROM day.leave " +
            "WHERE number = #{empId} AND status = '已批准' " +
            "AND DATE(start_date) BETWEEN #{start} AND #{end}")
    Integer countLeaveDays(@Param("empId") int empId, @Param("start") LocalDate start, @Param("end") LocalDate end);
}
