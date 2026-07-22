package com.oa7.dao;

import com.oa7.pojo.AttendanceRule;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Mapper
public interface AttendanceRuleDao {

    /**
     * 查询全局默认规则 (dept_id IS NULL AND enabled=1)
     */
    @Select("SELECT * FROM day.attendance_rule WHERE dept_id IS NULL AND enabled = 1 LIMIT 1")
    AttendanceRule selectDefault();

    /**
     * 查询指定部门的启用规则 (dept_id=#{deptId} AND enabled=1)
     */
    @Select("SELECT * FROM day.attendance_rule WHERE dept_id = #{deptId} AND enabled = 1")
    AttendanceRule selectByDept(@Param("deptId") int deptId);

    /**
     * 查询所有规则 (全局默认排在最前，其余按规则名称排序)
     */
    @Select("SELECT * FROM day.attendance_rule ORDER BY ISNULL(dept_id) DESC, rule_name")
    List<AttendanceRule> selectAll();

    /**
     * 根据 ID 查询规则
     */
    @Select("SELECT * FROM day.attendance_rule WHERE id = #{id}")
    AttendanceRule selectById(@Param("id") int id);

    /**
     * 插入规则
     */
    @Insert("INSERT INTO day.attendance_rule(rule_name, dept_id, work_start_time, work_end_time, " +
            "late_threshold_min, early_threshold_min, enabled, created_at, updated_at) " +
            "VALUES(#{ruleName}, #{deptId}, #{workStartTime}, #{workEndTime}, " +
            "#{lateThresholdMin}, #{earlyThresholdMin}, #{enabled}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AttendanceRule rule);

    /**
     * 更新规则
     */
    @Update("UPDATE day.attendance_rule SET rule_name = #{ruleName}, dept_id = #{deptId}, " +
            "work_start_time = #{workStartTime}, work_end_time = #{workEndTime}, " +
            "late_threshold_min = #{lateThresholdMin}, early_threshold_min = #{earlyThresholdMin}, " +
            "enabled = #{enabled}, updated_at = NOW() WHERE id = #{id}")
    int update(AttendanceRule rule);

    /**
     * 删除规则（物理删除）
     */
    @Delete("DELETE FROM day.attendance_rule WHERE id = #{id}")
    int delete(@Param("id") int id);
}
