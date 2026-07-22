package com.oa7.service.Impl;

import com.oa7.dao.HolidayDao;
import com.oa7.pojo.Holiday;
import com.oa7.service.HolidayService;
import com.oa7.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class HolidayServiceImpl implements HolidayService {

    @Autowired
    private HolidayDao holidayDao;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public RESP getByYear(int year) {
        List<Holiday> list = holidayDao.selectByYear(year);
        return RESP.ok(list);
    }

    @Override
    public RESP getByDateRange(LocalDate start, LocalDate end) {
        if (start.isAfter(end)) {
            return RESP.error("开始日期不能晚于结束日期");
        }
        List<Holiday> list = holidayDao.selectByDateRange(start, end);
        return RESP.ok(list);
    }

    @Override
    public RESP update(String dateStr, String type, String description) {
        LocalDate date;
        try {
            date = LocalDate.parse(dateStr, DATE_FMT);
        } catch (Exception e) {
            return RESP.error("日期格式错误，请使用 yyyy-MM-dd 格式");
        }

        // 校验 type 合法性
        if (!"WORKDAY".equals(type) && !"HOLIDAY".equals(type) && !"REST_DAY".equals(type)) {
            return RESP.error("type 必须为 WORKDAY/HOLIDAY/REST_DAY 之一");
        }

        // 检查是否存在，存在则更新，不存在则插入
        Holiday existing = holidayDao.selectByDate(date);
        if (existing != null) {
            existing.setType(type);
            existing.setDescription(description != null ? description : existing.getDescription());
            holidayDao.update(existing);
        } else {
            Holiday holiday = new Holiday();
            holiday.setDate(date);
            holiday.setType(type);
            holiday.setDescription(description != null ? description : "");
            holiday.setYear(date.getYear());
            holidayDao.insertOrUpdate(holiday);
        }

        return RESP.ok("操作成功");
    }

    @Override
    public RESP batchImport(int year) {
        if (year < 2000 || year > 2100) {
            return RESP.error("年份范围必须在 2000-2100 之间");
        }

        // 获取数据库中已有的日期
        List<Holiday> existingList = holidayDao.selectByYear(year);
        Set<LocalDate> existingDates = existingList.stream()
                .map(Holiday::getDate)
                .collect(Collectors.toSet());

        // 生成全年日期，排除已存在的
        List<Holiday> toInsert = new ArrayList<>();
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);

        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            if (!existingDates.contains(date)) {
                Holiday holiday = new Holiday();
                holiday.setDate(date);
                holiday.setYear(year);
                // 根据周末判断类型
                if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
                    holiday.setType("REST_DAY");
                    holiday.setDescription("周末");
                } else {
                    holiday.setType("WORKDAY");
                    holiday.setDescription("工作日");
                }
                toInsert.add(holiday);
            }
        }

        if (toInsert.isEmpty()) {
            return RESP.ok("该年份数据已完整，无需导入");
        }

        // 分批插入，每批 100 条
        int batchSize = 100;
        int total = 0;
        for (int i = 0; i < toInsert.size(); i += batchSize) {
            int endIdx = Math.min(i + batchSize, toInsert.size());
            List<Holiday> batch = toInsert.subList(i, endIdx);
            total += holidayDao.batchInsert(batch);
        }

        return RESP.ok("成功导入 " + total + " 条数据（跳过 " + existingDates.size() + " 条已存在的记录）");
    }

    @Override
    public RESP getCalendar(int year) {
        List<Holiday> list = holidayDao.selectByYear(year);

        // 如果数据不完整，自动补全缺失的日期
        int expectedDays = LocalDate.of(year, 12, 31).getDayOfYear();
        if (list.size() < expectedDays) {
            batchImport(year);
            list = holidayDao.selectByYear(year);
        }

        return RESP.ok(list);
    }
}
