package com.oa2.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface AdminDao {

    @Select("SELECT id FROM day.admin")
    List<Integer> selectAllIds();

    /**
     * 根据申请人编号获取应接收通知的管理员ID列表
     * 规则：
     * - 董事长(duty_id=17) → 所有申请都通知
     * - 人事部部长(dept_id=1, duty_id=1) → 所有申请都通知
     * - 本部门部长/副部长(duty_id=1或2) → 仅当部门匹配申请人
     */
    @Select("SELECT a.id FROM day.admin a " +
            "INNER JOIN day.emp e ON e.number = a.emp_number " +
            "WHERE a.emp_number IS NOT NULL " +
            "AND (e.duty_id = 17 " +
            "     OR (e.dept_id = 1 AND e.duty_id = 1) " +
            "     OR (e.duty_id IN (1, 2) AND e.dept_id = " +
            "         (SELECT dept_id FROM day.emp WHERE number = #{applicantNumber})))")
    List<Integer> selectNotifyTargetIds(@Param("applicantNumber") int applicantNumber);
}
