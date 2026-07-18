package com.oa2.service.impl;

import com.github.pagehelper.PageInfo;
import com.oa2.dao.EmpDao;
import com.oa2.pojo.Emp;
import com.oa2.pojo.Sign;
import com.oa2.repository.SignElasticsearchRepository;
import com.oa2.service.SignService;
import com.oa2.util.DU;
import com.oa2.util.LocationUtil;
import com.oa2.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class SignServiceElasticsearchImpl implements SignService {

    @Autowired
    private SignElasticsearchRepository signRepository;

    @Autowired
    private EmpDao empDao;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    // 获取当前员工签到记录
    @Override
    public RESP empSignList(HttpSession session) {

        System.out.println("获取当前员工签到记录");
        // 1. 获取当前员工数据
        Emp emp = (Emp) session.getAttribute("emp");
        if (emp == null) {
            return RESP.error("用户未登录");
        }
        // 2. 获取员工的当天的日期
        String today = DU.getNowSortString();

        // 3. 查询当天打卡记录
        List<Sign> list = signRepository.findByNumberAndSignDateStartsWith(emp.getNumber(), today);
        // 说明当前员工今天没有签到信息，需要为当前员工创建签到任务
        if (list.isEmpty()) {
            // 检查是否已经存在今日的签到记录（使用dateOnly字段）
            List<Sign> existingRecords = signRepository.findByNumberAndDateOnly(emp.getNumber(), today);

            if (existingRecords.isEmpty()) {
                // 只为当前员工创建今日的签到记录
                // 创建上午签到记录
                Sign morningSign = createSignRecord(emp.getNumber(), DU.getNowAM(), "未签到", "a");
                signRepository.save(morningSign);

                // 创建下午签到记录  
                Sign afternoonSign = createSignRecord(emp.getNumber(), DU.getNowPM(), "未签到", "p");
                signRepository.save(afternoonSign);
                
                // 重新查询当天打卡记录
                list = signRepository.findByNumberAndSignDateStartsWith(emp.getNumber(), today);
            } else {
                list = existingRecords;
            }
        }
        // 补充员工信息（姓名、部门）
        list = enrichSignRecords(list);

        return RESP.ok(list);
    }

    @Override
    public RESP selectByPagehelper(int currentPage, int pageSize, HttpSession session) {
        return selectByPage(currentPage, pageSize, session);
    }

    //分页查询员工已签到记录
    @Override
    public RESP selectByPage(int currentPage, int pageSize, HttpSession session) {
        // 1. 查询当前员工信息
        Emp emp = (Emp) session.getAttribute("emp");
        if (emp == null) {
            return RESP.error("用户未登录");
        }
        // 2. 使用 Elasticsearch 分页查询
        Pageable pageable = PageRequest.of(currentPage - 1, pageSize);
        Page<Sign> page = signRepository.findByNumberOrderByTimestampDesc(emp.getNumber(), pageable);

        //测试Page集合中数据 例如： 员工编号: 145, 签到时间: 2025-07-02 09:43:25:913, 状态: 已签到 ......
        List<Sign> list = page.getContent();

        // 补充员工信息(补充对应的部门信息)
        list = enrichSignRecords(list);
        // 获取总条数
        long total = signRepository.countByNumber(emp.getNumber());

        return RESP.ok(list,currentPage, (int) total);
    }


    @Override
    public RESP updateState(Sign sign, HttpSession session, String coordinates) {
        try {
            Emp emp = (Emp) session.getAttribute("emp");
            if (emp == null) {
                return RESP.error("用户未登录");
            }

            // 设置员工编号
            sign.setNumber(emp.getNumber());
            // 查找今天该员工该类型的签到记录
            String today = DU.getNowSortString(); // 获取今天日期 yyyy-MM-dd
            // 先查询今天所有记录，然后筛选类型
            List<Sign> todayRecords = signRepository.findByNumberAndDateOnly(emp.getNumber(), today);
            Sign existingSign = null;
            
            // 查找对应类型的记录
            for (Sign record : todayRecords) {
                if (sign.getType().equals(record.getType())) {
                    existingSign = record;
                    break;
                }
            }
            if (existingSign != null) {
                // 检查是否已经签到过
                if ("已签到".equals(existingSign.getState())) {
                    return RESP.error("今日已" + (sign.getType().equals("a") ? "签到" : "签退") + "，不可重复操作");
                }
                // 更新签到状态
                existingSign.setState("已签到");
                existingSign.setSignDate(DU.formatDateToString(new Date()));
                
                // 解析地理位置
                if (coordinates != null && !coordinates.isEmpty()) {
                    // 验证坐标格式
                    if (LocationUtil.isValidCoordinates(coordinates)) {
                        try {
                            String address = LocationUtil.getAddressFromCoordinates(coordinates);
                            existingSign.setSign_address(address);
                        } catch (Exception e) {
                            System.err.println("地址解析异常：" + e.getMessage());
                            existingSign.setSign_address("位置解析失败");
                        }
                    } else {
                        existingSign.setSign_address("坐标格式错误");
                    }
                } else {
                    existingSign.setSign_address("未获取到位置信息");
                }
                // 更新时间戳
                existingSign.setTimestamp(System.currentTimeMillis());
                existingSign.setDateOnly(today);
                // 补充员工信息
                existingSign.setName(emp.getName());
                existingSign.setDept_name(emp.getDept_name());

                signRepository.save(existingSign);
                return RESP.ok((sign.getType().equals("a") ? "签到" : "签退") + "成功");
            } else {
                return RESP.error("未找到今日的" + (sign.getType().equals("a") ? "签到" : "签退") + "任务，请联系管理员");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return RESP.error("签到失败：" + e.getMessage());
        }
    }

    /**
     * 创建签到记录
     */
    private Sign createSignRecord(int empNumber, String signDate, String state, String type) {
        Sign sign = new Sign();
        sign.setId(UUID.randomUUID().toString());
        sign.setSignDate(signDate);
        sign.setNumber(empNumber);
        sign.setState(state);
        sign.setType(type);
        sign.setTimestamp(System.currentTimeMillis());
        sign.setDateOnly(dateFormat.format(new Date()));
        sign.setTag(0);
        return sign;
    }

    /**
     * 补充签到记录的员工信息（姓名、部门）
     */
    private List<Sign> enrichSignRecords(List<Sign> signs) {
        for (Sign sign : signs) {
            try {
                // 根据员工编号查询员工信息
                Emp emp = empDao.selectByEmpNumber(sign.getNumber());
                if (emp != null) {
                    sign.setName(emp.getName());
                    sign.setDept_name(emp.getDept_name());
                }
            } catch (Exception e) {
                // 如果查询失败，设置默认值
                sign.setName("未知员工");
                sign.setDept_name("未知部门");
            }
        }
        return signs;
    }
} 