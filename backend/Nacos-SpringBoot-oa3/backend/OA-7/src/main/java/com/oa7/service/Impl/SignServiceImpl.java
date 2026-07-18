package com.oa7.service.Impl;

import com.oa7.dao.SignDao;
import com.oa7.pojo.O;
import com.oa7.pojo.Sign;
import com.oa7.service.SignService;
import com.oa7.util.DU;
import com.oa7.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class SignServiceImpl implements SignService {

    @Autowired
    private SignDao signDao;

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
        List<Sign> allRecords = signDao.selectAll();
        TreeMap<String, int[]> map = new TreeMap<>(Comparator.reverseOrder());
        for (Sign s : allRecords) {
            String date = s.getSignDate().substring(0, 10);
            map.putIfAbsent(date, new int[2]);
            if ("已签到".equals(s.getState())) {
                map.get(date)[0]++;
            } else {
                map.get(date)[1]++;
            }
        }
        List<O> allStats = new ArrayList<>();
        for (Map.Entry<String, int[]> e : map.entrySet()) {
            O o = new O();
            o.setDate(e.getKey());
            o.setYc(e.getValue()[0]);
            o.setNc(e.getValue()[1]);
            allStats.add(o);
        }
        int total = allStats.size();
        int start = (currentPage - 1) * pageSize;
        int end = Math.min(start + pageSize, total);
        if (start >= total) {
            return RESP.ok(Collections.emptyList(), currentPage, total);
        }
        return RESP.ok(allStats.subList(start, end), currentPage, total);
    }

    @Override
    public RESP dailyDetails(String date) {
        int morningSigned = signDao.countByDayByStateAndTypeYes(date, "a");
        int morningUnsigned = signDao.countByDayByStateAndTypeNo(date, "a");
        int eveningSigned = signDao.countByDayByStateAndTypeYes(date, "p");
        int eveningUnsigned = signDao.countByDayByStateAndTypeNo(date, "p");

        Map<String, Integer> result = new HashMap<>();
        result.put("morningSignedCount", morningSigned);
        result.put("morningUnsignedCount", morningUnsigned);
        result.put("eveningSignedCount", eveningSigned);
        result.put("eveningUnsignedCount", eveningUnsigned);
        result.put("totalSignedCount", morningSigned + eveningSigned);
        result.put("totalUnsignedCount", morningUnsigned + eveningUnsigned);

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
