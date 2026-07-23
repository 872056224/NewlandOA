package com.oa7.dao;

import com.oa7.pojo.RetroactiveSign;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface RetroactiveSignDao {

    @Select("SELECT * FROM day.retroactive_sign WHERE status='待审批' ORDER BY id DESC")
    List<RetroactiveSign> selectPending();

    @Update("UPDATE day.retroactive_sign SET status=#{status} WHERE id=#{id}")
    int updateStatus(@Param("id") int id, @Param("status") String status);

    /** 带乐观锁的状态更新 */
    @Update("UPDATE day.retroactive_sign SET status=#{status}, version=version+1 WHERE id=#{id} AND version=#{version}")
    int updateStatusWithVersion(@Param("id") int id, @Param("status") String status, @Param("version") int version);

    @Select("SELECT * FROM day.retroactive_sign WHERE id=#{id}")
    RetroactiveSign selectById(@Param("id") int id);

    /** 按部门查询待审批补签（联查 emp 表） */
    @Select("SELECT rs.* FROM day.retroactive_sign rs " +
            "INNER JOIN day.emp e ON e.number = rs.number " +
            "WHERE rs.status='待审批' AND e.dept_id = #{deptId} " +
            "ORDER BY rs.id DESC")
    List<RetroactiveSign> selectPendingByDept(@Param("deptId") int deptId);

    /** 按员工编号查询部门（联查） */
    @Select("SELECT e.dept_id FROM day.emp e " +
            "INNER JOIN day.retroactive_sign rs ON rs.number = e.number " +
            "WHERE rs.id = #{id}")
    Integer selectDeptIdBySignId(@Param("id") int id);
}
