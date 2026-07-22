package com.oa2.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface AdminDao {

    @Select("SELECT id FROM day.admin")
    List<Integer> selectAllIds();
}
