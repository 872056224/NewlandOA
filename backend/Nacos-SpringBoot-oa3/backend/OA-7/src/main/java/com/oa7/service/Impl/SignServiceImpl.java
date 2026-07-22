package com.oa7.service.Impl;

import com.oa7.constant.AttendanceStatus;
import com.oa7.constant.TodayStatus;
import com.oa7.dao.AttendanceDao;
import com.oa7.dao.HolidayDao;
import com.oa7.dao.SignDao;
import com.oa7.pojo.Attendance;
import com.oa7.pojo.O;
import com.oa7.pojo.Sign;
import com.oa7.service.SignService;
import com.oa7.util.DU;
import com.oa7.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;

@Service
public class SignServiceImpl implements SignService {

    @Autowired
    private SignDao signDao;

    @Autowired
    private AttendanceDao attendanceDao;

    @Autowired
    private HolidayDao holidayDao;

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
        // 1. Get all WORKDAY dates from holiday table, ordered DESC
        List<LocalDate> workdayDates = holidayDao.selectAllWorkdayDates();
        int total = workdayDates.size();

        // 2. Paginate
        int start = (currentPage - 1) * pageSize;
        int end = Math.min(start + pageSize, total);
        if (start >= total) {
            return RESP.ok(Collections.emptyList(), currentPage, total);
        }
        List<LocalDate> pageDates = workdayDates.subList(start, end);

        // 3. For each date, query attendance records and compute stats
        List<O> statsList = new ArrayList<>();
        for (LocalDate date : pageDates) {
            List<Attendance> records = attendanceDao.selectByDate(date);
            int totalEmployees = records.size();
            int onLeave = 0;
            int signed = 0;
            for (Attendance a : records) {
                if (a.getAttendanceStatus() == AttendanceStatus.LEAVE) {
                    onLeave++;
                }
                if (a.getTodayStatus() == TodayStatus.CHECKED_IN
                        || a.getTodayStatus() == TodayStatus.CHECKED_OUT
                        || a.getAttendanceStatus() == AttendanceStatus.NORMAL) {
                    signed++;
                }
            }
            int unsigned = totalEmployees - signed - onLeave;

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
        LocalDate localDate = LocalDate.parse(date);
        List<Attendance> records = attendanceDao.selectByDate(localDate);
        int totalEmployees = records.size();
        int onLeave = 0;
        int signed = 0;
        for (Attendance a : records) {
            if (a.getAttendanceStatus() == AttendanceStatus.LEAVE) {
                onLeave++;
            }
            if (a.getTodayStatus() == TodayStatus.CHECKED_IN
                    || a.getTodayStatus() == TodayStatus.CHECKED_OUT
                    || a.getAttendanceStatus() == AttendanceStatus.NORMAL) {
                signed++;
            }
        }
        int unsigned = totalEmployees - signed - onLeave;

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
        List<Sign> allRecords = signDao.selectAll();
        List<String> dates = new ArrayList<>();
        List<Integer> signed = new ArrayList<>();
        List<Integer> unsigned = new ArrayList<>();
        List<Integer> total = new ArrayList<>();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar cal = Calendar.getInstance();

        for (int i = 4; i >= 0; i--) {
            Calendar dayCal = (Calendar) cal.clone();
            dayCal.add(Calendar.DAY_OF_YEAR, -i);
            String dateStr = sdf.format(dayCal.getTime());
            dates.add(dateStr);

            int signedCount = 0;
            int unsignedCount = 0;
            for (Sign s : allRecords) {
                if (s.getSignDate() != null && s.getSignDate().startsWith(dateStr)) {
                    if ("已签到".equals(s.getState())) {
                        signedCount++;
                    } else {
                        unsignedCount++;
                    }
                }
            }
            signed.add(signedCount);
            unsigned.add(unsignedCount);
            total.add(signedCount + unsignedCount);
        }

        return RESP.ok(dates, signed, unsigned, total);
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
