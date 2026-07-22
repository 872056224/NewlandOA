package com.oa7.service;

import com.oa7.util.RESP;

import java.time.LocalDate;

public interface HolidayService {

    RESP getByYear(int year);

    RESP getByDateRange(LocalDate start, LocalDate end);

    RESP update(String dateStr, String type, String description);

    RESP batchImport(int year);

    RESP getCalendar(int year);
}
