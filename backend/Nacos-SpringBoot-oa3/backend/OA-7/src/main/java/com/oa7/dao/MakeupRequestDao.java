package com.oa7.dao;

import com.oa7.pojo.MakeupRequest;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface MakeupRequestDao {

    @Select("SELECT * FROM day.makeup_request WHERE status='PENDING' ORDER BY id DESC")
    @Results(id = "makeupResult", value = {
        @Result(property = "id", column = "id"),
        @Result(property = "empId", column = "emp_id"),
        @Result(property = "date", column = "date"),
        @Result(property = "type", column = "type"),
        @Result(property = "requestTime", column = "request_time"),
        @Result(property = "reason", column = "reason"),
        @Result(property = "status", column = "status"),
        @Result(property = "version", column = "version"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "updatedAt", column = "updated_at")
    })
    List<MakeupRequest> selectPending();

    @Select("SELECT * FROM day.makeup_request WHERE id=#{id}")
    @ResultMap("makeupResult")
    MakeupRequest selectById(@Param("id") int id);

    @Update("UPDATE day.makeup_request SET status=#{status}, version=version+1 WHERE id=#{id} AND version=#{version}")
    int updateStatusWithVersion(@Param("id") int id, @Param("status") String status, @Param("version") int version);

    @Select("SELECT COUNT(*) FROM day.makeup_request WHERE status='PENDING'")
    int countPending();

    /** 按部门查询待审批补卡 */
    @Select("SELECT mr.* FROM day.makeup_request mr " +
            "INNER JOIN day.emp e ON e.number = mr.emp_id " +
            "WHERE mr.status='PENDING' AND e.dept_id = #{deptId} " +
            "ORDER BY mr.id DESC")
    @ResultMap("makeupResult")
    List<MakeupRequest> selectPendingByDept(@Param("deptId") int deptId);

    /** 查询补卡申请所属部门 */
    @Select("SELECT e.dept_id FROM day.emp e " +
            "INNER JOIN day.makeup_request mr ON mr.emp_id = e.number " +
            "WHERE mr.id = #{id}")
    Integer selectDeptIdByRequestId(@Param("id") int id);
}
