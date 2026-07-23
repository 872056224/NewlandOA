package com.oa7.service;

import com.oa7.util.RESP;

public interface SalaryService {
    RESP calculate(String yearMonth);
    RESP getByMonth(String yearMonth);
    RESP getMySalary(int empId, String yearMonth);
}
