package com.oa2.dao;

import com.oa2.pojo.SalaryDetail;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface SalaryDao {

    @Select("SELECT sd.id, sd.emp_id AS empId, sd.`year_month` AS yearMonth, " +
            "sd.base_salary AS baseSalary, sd.work_days AS workDays, " +
            "sd.daily_wage AS dailyWage, sd.hourly_wage AS hourlyWage, " +
            "sd.total_missing_minutes AS totalMissingMinutes, " +
            "sd.missing_deduction AS missingDeduction, " +
            "sd.overtime_hours AS overtimeHours, sd.overtime_pay AS overtimePay, " +
            "sd.leave_days AS leaveDays, sd.leave_deduction AS leaveDeduction, " +
            "sd.final_salary AS finalSalary, sd.status, " +
            "sd.created_at AS createdAt, sd.updated_at AS updatedAt, " +
            "e.name AS empName, d.dept_name AS deptName, du.duty_name AS dutyName " +
            "FROM day.salary_detail sd " +
            "LEFT JOIN day.emp e ON e.number = sd.emp_id " +
            "LEFT JOIN day.department d ON d.dept_id = e.dept_id " +
            "LEFT JOIN day.duty du ON du.duty_id = e.duty_id " +
            "WHERE sd.emp_id = #{empId} AND sd.`year_month` = #{yearMonth}")
    SalaryDetail selectByEmpAndMonth(@Param("empId") int empId, @Param("yearMonth") String yearMonth);
}
