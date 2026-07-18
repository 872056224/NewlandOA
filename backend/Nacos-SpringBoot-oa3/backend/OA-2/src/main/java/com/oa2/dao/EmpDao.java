package com.oa2.dao;

import com.oa2.pojo.*;
import org.apache.ibatis.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface EmpDao {

    //通过员工编号查找员工信息
    @Select("select emp.*,dept_name,duty_name from " +
            "day.emp left join department on department.dept_id = emp.dept_id " +
            "left join duty on  emp.duty_id = duty.duty_id where number=#{number}  ")
    Emp selectByNumber(Emp emp);

    //通过员工编号直接查找员工信息
    @Select("select emp.*,dept_name,duty_name from " +
            "day.emp left join department on department.dept_id = emp.dept_id " +
            "left join duty on  emp.duty_id = duty.duty_id where number=#{number}  ")
    Emp selectByEmpNumber(int number);

    //通过员工编号更新员工信息
    @Update("update day.emp set name=#{name} ,birthday=#{birthday} ,address=#{address} where number=#{number} ")
    int updateEmp(Emp emp);

    //通过员工编号更新员工密码
    @Update("update day.emp set pwd=#{pwd}  where number=#{number} ")
    int updateEmpPwd(Emp emp);

    //获取每个员工的编号
    @Select("select number from day.emp")
    List<Integer> selectAllEmpNumber();

}
