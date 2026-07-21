package com.oa7.service.Impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.oa7.dao.NotificationDao;
import com.oa7.dao.RetroactiveSignDao;
import com.oa7.dao.SignDao;
import com.oa7.pojo.RetroactiveSign;
import com.oa7.service.RetroactiveSignService;
import com.oa7.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RetroactiveSignServiceImpl implements RetroactiveSignService {

    @Autowired
    private RetroactiveSignDao retroactiveSignDao;

    @Autowired
    private SignDao signDao;

    @Autowired
    private NotificationDao notificationDao;

    @Override
    public RESP getPending(int currentPage, int pageSize) {
        PageHelper.startPage(currentPage, pageSize);
        List<RetroactiveSign> list = retroactiveSignDao.selectPending();
        PageInfo<RetroactiveSign> pageInfo = new PageInfo<>(list);
        return RESP.ok(list, pageInfo.getPageNum(), (int) pageInfo.getTotal());
    }

    @Override
    public RESP approve(int id) {
        retroactiveSignDao.updateStatus(id, "已批准");

        RetroactiveSign sign = retroactiveSignDao.selectById(id);
        if (sign != null) {
            signDao.updateStateByDateAndType(sign.getNumber(), sign.getSign_date(), sign.getType());
            String typeLabel = sign.getType().equals("a") ? "上午" : "下午";
            notificationDao.insert("retroactive_approved", "补签已批准",
                    "您在 " + sign.getSign_date() + "(" + typeLabel + ") 的补签申请已获批准",
                    sign.getNumber(), String.valueOf(id));
        }

        return RESP.ok("操作成功");
    }

    @Override
    public RESP reject(int id) {
        retroactiveSignDao.updateStatus(id, "已拒绝");

        RetroactiveSign sign = retroactiveSignDao.selectById(id);
        if (sign != null) {
            String typeLabel = sign.getType().equals("a") ? "上午" : "下午";
            notificationDao.insert("retroactive_rejected", "补签已拒绝",
                    "您在 " + sign.getSign_date() + "(" + typeLabel + ") 的补签申请已被拒绝",
                    sign.getNumber(), String.valueOf(id));
        }

        return RESP.ok("操作成功");
    }
}
