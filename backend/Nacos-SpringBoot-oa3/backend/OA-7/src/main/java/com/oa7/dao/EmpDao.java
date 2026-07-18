package com.oa7.dao;

import com.oa7.pojo.*;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @name: chenle
 * @Date: 2021/11/30 21:59
 * @Author: IAO
 * @Description: ...
 */
@Repository
@Mapper
public interface EmpDao {

    @Select("select * from day.emp where name=#{name}   ")
    Emp selectByName(Emp emp);

    //获取员工列表
    @Select("select emp.*,dept_name,duty_name from " +
            "day.emp left join department on department.dept_id = emp.dept_id " +
            "left join duty on emp.duty_id = duty.duty_id order by number limit #{a},#{b} ")
    List<Emp> selectByPage(@Param("a") int a, @Param("b") int b);


    @Select("select emp.*,dept_name,duty_name from " +
            "day.emp left join department on department.dept_id = emp.dept_id " +
            "left join duty on emp.duty_id = duty.duty_id order by number ")
    List<Emp> selectByPageHelper();



    //统计员工人数
    @Select("select count(*) from day.emp")
    int countUser();

    //更新员工信息
    @Update("update day.emp set name=#{name} ,birthday=#{birthday} ,address=#{address} ,dept_id=#{dept_id} ,duty_id=#{duty_id} where number=#{number} ")
    int updateEmp(Emp emp);

    //删除员工信息
    @Delete("delete from day.emp where number=#{number} ")
    int deleteEmp(Emp emp);

    //    删除员工考勤信息
    @Delete("delete from sign where number=#{number} ")
    int deleteEmpSignByNumber(Emp emp);

    //添加员工
    @Insert("insert into day.emp (name, birthday, address, dept_id, duty_id) VALUES (#{name} ,#{birthday} ,#{address} ,#{dept_id} ,#{duty_id}  )")
    int addEmp(Emp emp);

    //查询部门列表
    @Select("select * from day.department")
    List<Department> getDeptData();

    //查询职务列表
    @Select("select * from day.duty")
    List<Duty> getDutyData();



    //更新职务信息
    @Update("update day.emp set dept_id=#{dept_id} ,duty_id=#{duty_id}  where number=#{number} ")
    int updateDD(Emp emp);

    //获取每个员工的编号
    @Select("select number from day.emp")
    List<Integer> selectAllEmpNumber();

    //根据员工编号查询员工信息
    @Select("select emp.*,dept_name,duty_name from " +
            "day.emp left join department on department.dept_id = emp.dept_id " +
            "left join duty on emp.duty_id = duty.duty_id where number=#{number}")
    Emp selectByEmpNumber(int number);
}
