package com.oa7.service.Impl;

import com.oa7.constant.AttendanceStatus;
import com.oa7.constant.TodayStatus;
import com.oa7.dao.AttendanceDao;
import com.oa7.dao.EmpDao;
import com.oa7.dao.HolidayDao;
import com.oa7.dao.LeaveDao;
import com.oa7.dao.SignDao;
import com.oa7.pojo.Attendance;
import com.oa7.pojo.O;
import com.oa7.pojo.Sign;
import com.oa7.service.SignService;
import com.oa7.util.DU;
import com.oa7.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SignServiceImpl implements SignService {

    @Autowired
    private SignDao signDao;

    @Autowired
    private AttendanceDao attendanceDao;

    @Autowired
    private HolidayDao holidayDao;

    @Autowired
    private EmpDao empDao;

    @Autowired
    private LeaveDao leaveDao;

    /** 缓存员工总数（每日刷新） */
    private int getTotalEmployeeCount() {
        return empDao.countUser();
    }

    @Override
    public RESP todaySigned(int currentPage, int pageSize) {
        String today = DU.getNowSortString();
        int offset = (currentPage - 1) * pageSize;
        List<Sign> list = signDao.selectToDayYesByPage(offset, pageSize, today);
        int total = signDao.countToDayYes(today);
        return RESP.ok(list, currentPage, total);
    }

    @Override
    public RESP dailyStatistics(int currentPage, int pageSize) {
        LocalDate today = LocalDate.now();
        int totalEmployees = getTotalEmployeeCount();

        // WORKDAY dates up to today
        List<LocalDate> workdayDates = holidayDao.selectAllWorkdayDates().stream()
                .filter(d -> !d.isAfter(today))
                .collect(Collectors.toList());
        int total = workdayDates.size();

        int start = (currentPage - 1) * pageSize;
        int end = Math.min(start + pageSize, total);
        if (start >= total) {
            return RESP.ok(Collections.emptyList(), currentPage, total);
        }
        List<LocalDate> pageDates = workdayDates.subList(start, end);

        List<O> statsList = new ArrayList<>();
        for (LocalDate date : pageDates) {
            // 请假人数：从 leave 表统计当天已批准的请假（去重员工）
            int onLeave = leaveDao.countApprovedLeaveByDate(date.toString());

            // 签到人数：从 attendance 表统计当天有签到/签退记录的员工
            List<Attendance> records = attendanceDao.selectByDate(date);
            int signed = 0;
            Set<Integer> signedEmpIds = new HashSet<>();
            for (Attendance a : records) {
                if (a.getCheckInTime() != null) {
                    signedEmpIds.add(a.getEmpId());
                }
            }
            signed = signedEmpIds.size();

            // 未签到 = 总人数 - 请假 - 已签到
            int unsigned = Math.max(0, totalEmployees - onLeave - signed);

            O o = new O();
            o.setDate(date.toString());
            o.setTotalEmployees(totalEmployees);
            o.setOnLeave(onLeave);
            o.setSigned(signed);
            o.setUnsigned(unsigned);
            statsList.add(o);
        }

        return RESP.ok(statsList, currentPage, total);
    }

    @Override
    public RESP dailyDetails(String date) {
        int totalEmployees = getTotalEmployeeCount();
        int onLeave = leaveDao.countApprovedLeaveByDate(date);

        LocalDate localDate = LocalDate.parse(date);
        List<Attendance> records = attendanceDao.selectByDate(localDate);
        Set<Integer> signedEmpIds = new HashSet<>();
        for (Attendance a : records) {
            if (a.getCheckInTime() != null) {
                signedEmpIds.add(a.getEmpId());
            }
        }
        int signed = signedEmpIds.size();
        int unsigned = Math.max(0, totalEmployees - onLeave - signed);

        Map<String, Object> result = new HashMap<>();
        result.put("totalEmployees", totalEmployees);
        result.put("onLeave", onLeave);
        result.put("signed", signed);
        result.put("unsigned", unsigned);
        result.put("expected", totalEmployees - onLeave);

        return RESP.ok(result);
    }

    @Override
    public RESP chartData() {
        LocalDate today = LocalDate.now();
        int totalEmployees = getTotalEmployeeCount();

        // 4 个最近的工作日（截止到今天）
        List<LocalDate> workdayDates = holidayDao.selectAllWorkdayDates().stream()
                .filter(d -> !d.isAfter(today))
                .limit(4)
                .collect(Collectors.toList());
        Collections.reverse(workdayDates);

        List<String> dateLabels = new ArrayList<>();
        List<Integer> signedData = new ArrayList<>();
        List<Integer> unsignedData = new ArrayList<>();
        List<Integer> leaveData = new ArrayList<>();

        for (LocalDate date : workdayDates) {
            int onLeave = leaveDao.countApprovedLeaveByDate(date.toString());

            List<Attendance> records = attendanceDao.selectByDate(date);
            Set<Integer> signedEmpIds = new HashSet<>();
            for (Attendance a : records) {
                if (a.getCheckInTime() != null) {
                    signedEmpIds.add(a.getEmpId());
                }
            }
            int signedCount = signedEmpIds.size();

            dateLabels.add(date.toString());
            signedData.add(signedCount);
            unsignedData.add(Math.max(0, totalEmployees - onLeave - signedCount));
            leaveData.add(onLeave);
        }

        return RESP.ok(dateLabels, signedData, unsignedData, leaveData);
    }

    @Override
    public RESP unsigned(int currentPage, int pageSize) {
        int offset = (currentPage - 1) * pageSize;
        List<Sign> list = signDao.selectNoByPage(offset, pageSize);
        int total = signDao.countUserNo();
        return RESP.ok(list, currentPage, total);
    }

    @Override
    public RESP todayUnsigned(int currentPage, int pageSize) {
        String today = DU.getNowSortString();
        int offset = (currentPage - 1) * pageSize;
        List<Sign> list = signDao.selectToDayNoByPage(offset, pageSize, today);
        int total = signDao.countToDayNo(today);
        return RESP.ok(list, currentPage, total);
    }

    @Override
    public String approve(int id) {
        List<Sign> all = signDao.selectAll();
        for (Sign s : all) {
            if (Integer.parseInt(s.getId()) == id) {
                s.setState("已签到");
                signDao.updateState(s, DU.getNowString());
                return "true";
            }
        }
        return "false";
    }
}
