package com.oa7.service;

import com.oa7.pojo.Sign;
import com.oa7.util.RESP;

import javax.servlet.http.HttpSession;

/**
 * @name: chenle
 * @Date: 2021/12/3 14:44
 * @Author: IAO
 * @Description: ...
 */
public interface SignService {

    RESP todaySigned(int currentPage, int pageSize);

    RESP dailyStatistics(int currentPage, int pageSize);

    RESP dailyDetails(String date);

    RESP chartData();

    RESP unsigned(int currentPage, int pageSize);

    RESP todayUnsigned(int currentPage, int pageSize);

    String approve(int id);

}
