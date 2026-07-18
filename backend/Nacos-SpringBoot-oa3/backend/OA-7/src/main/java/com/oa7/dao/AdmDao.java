package com.oa7.dao;

import com.oa7.pojo.Admin;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * @name: chenle
 * @Date: 2021/12/1 20:04
 * @Author: IAO
 * @Description: ...
 */
@Mapper
public interface AdmDao {

    //通过名字查询管理员信息
    @Select("select * from day.admin where name=#{name} ")
    Admin selectByName(Admin admin);

    //管理员注册
    @Insert("insert into day.admin (name, pwd) VALUES (#{name} ,#{pwd} )")
    int insertAdm(Admin admin);




}
