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
}
