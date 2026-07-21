package com.oa2.service.impl;

import com.oa2.dao.RetroactiveSignDao;
import com.oa2.pojo.RetroactiveSign;
import com.oa2.service.RetroactiveSignService;
import com.oa2.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RetroactiveSignServiceImpl implements RetroactiveSignService {

    @Autowired
    private RetroactiveSignDao retroactiveSignDao;

    @Override
    public RESP apply(int number, String signDate, String type, String reason) {
        RetroactiveSign sign = new RetroactiveSign();
        sign.setNumber(number);
        sign.setSign_date(signDate);
        sign.setType(type);
        sign.setReason(reason);
        sign.setStatus("待审批");

        int ret = retroactiveSignDao.insert(sign);
        if (ret > 0) {
            return RESP.ok("提交成功");
        }
        return RESP.error("提交失败");
    }

    @Override
    public RESP getMyList(int number, int currentPage, int pageSize) {
        int offset = (currentPage - 1) * pageSize;
        List<RetroactiveSign> list = retroactiveSignDao.selectByNumberPage(number, offset, pageSize);
        int total = retroactiveSignDao.countByNumber(number);
        return RESP.ok(list, currentPage, total);
    }
}
