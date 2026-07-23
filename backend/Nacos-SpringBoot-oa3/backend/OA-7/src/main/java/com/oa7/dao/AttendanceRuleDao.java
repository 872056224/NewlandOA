package com.oa7.dao;

import com.oa7.pojo.AttendanceRule;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Mapper
public interface AttendanceRuleDao {

    @Select("SELECT * FROM day.attendance_rule WHERE dept_id IS NULL AND enabled = 1 LIMIT 1")
    AttendanceRule selectDefault();

    @Select("SELECT * FROM day.attendance_rule WHERE dept_id = #{dept_id} AND enabled = 1")
    AttendanceRule selectByDept(@Param("dept_id") int deptId);

    @Select("SELECT * FROM day.attendance_rule ORDER BY ISNULL(dept_id) DESC, rule_name")
    List<AttendanceRule> selectAll();

    @Select("SELECT * FROM day.attendance_rule WHERE id = #{id}")
    AttendanceRule selectById(@Param("id") int id);

    @Insert("INSERT INTO day.attendance_rule(rule_name, dept_id, work_start_time, work_end_time, " +
            "late_threshold_min, early_threshold_min, enabled, created_at, updated_at) " +
            "VALUES(#{rule_name}, #{dept_id}, #{work_start_time}, #{work_end_time}, " +
            "#{late_threshold_min}, #{early_threshold_min}, #{enabled}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AttendanceRule rule);

    @Update("UPDATE day.attendance_rule SET rule_name = #{rule_name}, dept_id = #{dept_id}, " +
            "work_start_time = #{work_start_time}, work_end_time = #{work_end_time}, " +
            "late_threshold_min = #{late_threshold_min}, early_threshold_min = #{early_threshold_min}, " +
            "missing_tolerance_min = #{missing_tolerance_min}, enabled = #{enabled}, " +
            "updated_at = NOW() WHERE id = #{id}")
    int update(AttendanceRule rule);

    @Delete("DELETE FROM day.attendance_rule WHERE id = #{id}")
    int delete(@Param("id") int id);
}
