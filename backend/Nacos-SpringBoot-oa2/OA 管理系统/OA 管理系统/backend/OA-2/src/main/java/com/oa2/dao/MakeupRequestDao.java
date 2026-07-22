package com.oa2.dao;

import com.oa2.pojo.MakeupRequest;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface MakeupRequestDao {

    @Insert("INSERT INTO day.makeup_request(emp_id, date, type, request_time, reason, status) " +
            "VALUES(#{empId}, #{date}, #{type}, #{requestTime}, #{reason}, 'PENDING')")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(MakeupRequest request);

    @Select("SELECT * FROM day.makeup_request WHERE emp_id=#{empId} ORDER BY id DESC LIMIT #{offset}, #{limit}")
    List<MakeupRequest> selectByEmpPage(@Param("empId") int empId,
                                         @Param("offset") int offset,
                                         @Param("limit") int limit);

    @Select("SELECT count(*) FROM day.makeup_request WHERE emp_id=#{empId}")
    int countByEmp(@Param("empId") int empId);
}
