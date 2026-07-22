package com.oa7.dao;

import com.oa7.pojo.Admin;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AdmDao {

    @Select("select * from day.admin where name=#{name} ")
    Admin selectByName(Admin admin);

    @Insert("insert into day.admin (name, pwd) VALUES (#{name} ,#{pwd} )")
    int insertAdm(Admin admin);

    @Select("select id from day.admin")
    List<Integer> selectAllIds();
}
