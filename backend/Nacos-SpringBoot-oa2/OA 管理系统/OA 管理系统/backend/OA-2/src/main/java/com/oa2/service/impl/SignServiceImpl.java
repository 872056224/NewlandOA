package com.oa2.service.impl;

import com.oa2.dao.SignDao;
import com.oa2.pojo.Sign;
import com.oa2.service.SignService;
import com.oa2.util.LocationUtil;
import com.oa2.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SignServiceImpl implements SignService {

    @Autowired
    private SignDao signDao;

    @Override
    public RESP getMyRecordsPage(int number, int currentPage, int pageSize) {
        int offset = (currentPage - 1) * pageSize;
        List<Sign> records = signDao.selectByNumberPage(number, offset, pageSize);
        int total = signDao.countByNumber(number);
        return RESP.ok(records, currentPage, total);
    }

    @Override
    public RESP getMyRecords(int number) {
        List<Sign> records = signDao.selectByNumber(number);
        return RESP.ok(records);
    }

    @Override
    public RESP checkIn(Sign sign, String coordinates, String clientIp) {
        String address = null;

        // 优先使用浏览器定位坐标 → 转文字地址
        if (coordinates != null && !coordinates.isEmpty()) {
            address = LocationUtil.getAddressFromCoordinates(coordinates);
            if (address != null && (address.contains("错误") || address.contains("失败") || address.contains("异常"))) {
                address = null;
            }
        }

        // 没有地址 → 直接调腾讯IP定位（不传IP，自动检测服务器出口IP）
        if (address == null) {
            address = LocationUtil.getLocationByIp(null);
        }

        if (address != null) {
            sign.setSign_address(address);
        } else if (coordinates != null && !coordinates.isEmpty()) {
            sign.setSign_address(coordinates);
        }

        int ret = signDao.insert(sign);
        if (ret > 0) {
            return RESP.ok("签到成功");
        }
        return RESP.error("签到失败");
    }
}
