package com.oa7.service.Impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
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

    @Override
    public RESP getPending(int currentPage, int pageSize) {
        PageHelper.startPage(currentPage, pageSize);
        List<RetroactiveSign> list = retroactiveSignDao.selectPending();
        PageInfo<RetroactiveSign> pageInfo = new PageInfo<>(list);
        return RESP.ok(list, pageInfo.getPageNum(), (int) pageInfo.getTotal());
    }

    @Override
    public RESP approve(int id) {
        // 更新补签状态
        retroactiveSignDao.updateStatus(id, "已批准");

        // 更新对应的签到记录
        RetroactiveSign sign = retroactiveSignDao.selectById(id);
        if (sign != null) {
            signDao.updateStateByDateAndType(sign.getNumber(), sign.getSign_date(), sign.getType());
        }

        return RESP.ok("操作成功");
    }

    @Override
    public RESP reject(int id) {
        retroactiveSignDao.updateStatus(id, "已拒绝");
        return RESP.ok("操作成功");
    }
}
