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

    @Select("SELECT * FROM day.retroactive_sign WHERE id=#{id}")
    RetroactiveSign selectById(@Param("id") int id);
}
