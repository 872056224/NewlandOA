package com.oa7.service.Impl;

import com.alibaba.fastjson.JSON;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.oa7.pojo.Department;
import com.oa7.service.DeptService;
import com.oa7.dao.DeptDao;
import com.oa7.util.RESP;
import com.oa7.util.JediPoolUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import java.util.List;
import java.util.Set;

/**
 * @name: chenle
 * @Date: 2021/12/3 15:58
 * @Author: IAO
 * @Description: 部门管理服务实现 - 含Redis缓存与延迟双删策略
 */
@Service
public class DeptServiceImpl implements DeptService {

    private static final String CACHE_KEY_PREFIX = "oa:admin:departments:page:";
    private static final String CACHE_TOTAL_PREFIX = "oa:admin:departments:total:";
    private static final String CACHE_PATTERN = "oa:admin:departments:*";
    private static final int CACHE_TTL = 3600;

    @Autowired
    private DeptDao deptDao;

    @Autowired
    private JediPoolUtil jediPoolUtil;

    @Override
    public RESP selectByPage(int currentPage, int pageSize) {
        Jedis jedis = jediPoolUtil.getJedis();
        String key = CACHE_KEY_PREFIX + currentPage + ":" + pageSize;
        String totalKey = CACHE_TOTAL_PREFIX + currentPage + ":" + pageSize;

        try {
            // Try cache hit
            String cachedJson = jedis.get(key);
            if (cachedJson != null) {
                List<Department> list = JSON.parseArray(cachedJson, Department.class);
                String totalStr = jedis.get(totalKey);
                int total = totalStr != null ? Integer.parseInt(totalStr) : deptDao.countDept();
                return RESP.ok(list, currentPage, total);
            }

            // Cache miss - query DB with PageHelper
            PageHelper.startPage(currentPage, pageSize);
            List<Department> list = deptDao.selectByPageHelper();
            PageInfo<Department> pageInfo = new PageInfo<>(list);

            // Serialize list to JSON and store in Redis
            String json = JSON.toJSONString(pageInfo.getList());
            jedis.setex(key, CACHE_TTL, json);
            jedis.setex(totalKey, CACHE_TTL, String.valueOf(pageInfo.getTotal()));

            return RESP.ok(pageInfo.getList(), pageInfo.getPageNum(), (int) pageInfo.getTotal());
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback to DB on Redis error
            PageHelper.startPage(currentPage, pageSize);
            List<Department> list = deptDao.selectByPageHelper();
            PageInfo<Department> pageInfo = new PageInfo<>(list);
            return RESP.ok(pageInfo.getList(), pageInfo.getPageNum(), (int) pageInfo.getTotal());
        }
    }

    @Override
    public RESP add(Department dept, int currentPage, int pageSize) {
        Jedis jedis = jediPoolUtil.getJedis();

        try {
            // First deletion: remove all department cache keys
            deleteCacheByPattern(jedis, CACHE_PATTERN);

            // Execute DB operation
            deptDao.addDept(dept);

            // Delay 500ms to allow any concurrent read requests to finish
            Thread.sleep(500);

            // Second deletion: ensure no stale cache remains
            deleteCacheByPattern(jedis, CACHE_PATTERN);

            // Return refreshed page data
            return selectByPage(currentPage, pageSize);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return RESP.error("操作被中断");
        } catch (Exception e) {
            e.printStackTrace();
            return RESP.error("添加部门失败");
        }
    }

    @Override
    public RESP update(int deptId, Department dept, int currentPage, int pageSize) {
        Jedis jedis = jediPoolUtil.getJedis();

        try {
            // First deletion: remove all department cache keys
            deleteCacheByPattern(jedis, CACHE_PATTERN);

            // Set dept_id from path variable and execute DB operation
            dept.setDept_id(deptId);
            deptDao.updateDeptNameById(dept);

            // Delay 500ms to allow any concurrent read requests to finish
            Thread.sleep(500);

            // Second deletion: ensure no stale cache remains
            deleteCacheByPattern(jedis, CACHE_PATTERN);

            // Return refreshed page data
            return selectByPage(currentPage, pageSize);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return RESP.error("操作被中断");
        } catch (Exception e) {
            e.printStackTrace();
            return RESP.error("更新部门失败");
        }
    }

    /**
     * 根据通配符模式删除所有匹配的Redis缓存键
     */
    private void deleteCacheByPattern(Jedis jedis, String pattern) {
        Set<String> keys = jedis.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            jedis.del(keys.toArray(new String[0]));
        }
    }
}
