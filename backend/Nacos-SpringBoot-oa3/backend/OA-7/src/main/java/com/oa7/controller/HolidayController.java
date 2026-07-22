package com.oa7.controller;

import com.oa7.service.HolidayService;
import com.oa7.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/holidays")
@CrossOrigin
public class HolidayController {

    @Autowired
    private HolidayService holidayService;

    /**
     * 查询指定年份的所有节假日
     */
    @GetMapping("/year/{year}")
    public RESP getByYear(@PathVariable int year) {
        return holidayService.getByYear(year);
    }

    /**
     * 查询指定日期范围内的节假日
     */
    @GetMapping("/range")
    public RESP getByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return holidayService.getByDateRange(start, end);
    }

    /**
     * 更新指定日期的节假日类型
     */
    @PutMapping("/{date}")
    public RESP update(
            @PathVariable String date,
            @RequestParam String type,
            @RequestParam(required = false) String description) {
        return holidayService.update(date, type, description);
    }

    /**
     * 批量导入某一年份的节假日数据（自动填充缺失日期）
     */
    @PostMapping("/batch/{year}")
    public RESP batchImport(@PathVariable int year) {
        return holidayService.batchImport(year);
    }

    /**
     * 获取某一年份的完整日历（所有日期的类型）
     */
    @GetMapping("/calendar/{year}")
    public RESP getCalendar(@PathVariable int year) {
        return holidayService.getCalendar(year);
    }
}
