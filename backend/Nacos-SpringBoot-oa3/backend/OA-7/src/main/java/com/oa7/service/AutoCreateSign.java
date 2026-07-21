package com.oa7.service;

import com.oa7.dao.EmpDao;
import com.oa7.dao.LeaveDao;
import com.oa7.dao.SignDao;
import com.oa7.pojo.Sign;
import com.oa7.util.DU;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;

/**
 * @name: chenle
 * @Date: 2021/12/25 5:24
 * @Author: IAO
 * @Description: 定时自动更新员工考勤（跳过已请假员工）
 */
@Configuration
//开启定时任务
@EnableScheduling
public class AutoCreateSign {
    @Autowired
    private  SignDao signDao;
    @Autowired
    private EmpDao empDao;
    @Autowired
    private LeaveDao leaveDao;
    //每日凌晨零点执行，生成员工签到任务
    @Scheduled(cron = "0 0 0 * * ?")
    public void create() {
        //获取所有员工编号
        List<Integer> list = empDao.selectAllEmpNumber();
        for (int n:list){
            // 检查员工当天是否有已批准的请假
            int leaveCount = leaveDao.countApprovedLeaveToday(n, DU.getNowSortString());
            // 有请假则标记为已签到（已请假免打卡），否则标记未签到
            String state = leaveCount > 0 ? "已签到" : "未签到";

            Sign sign = new Sign();
            sign.setSignDate(DU.getNowAM());
            sign.setNumber(n);
            sign.setState(state);
            sign.setType("a");
            signDao.addSign(sign);
            sign.setSignDate(DU.getNowPM());
            sign.setType("p");
            signDao.addSign(sign);
        }

    }
}
