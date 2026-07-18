package com.oa7.service.Impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.oa7.dao.KbDocDao;
import com.oa7.pojo.KbDoc;
import com.oa7.service.KbDocService;
import com.oa7.util.RESP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class KbDocServiceImpl implements KbDocService {

    private static final Logger log = LoggerFactory.getLogger(KbDocServiceImpl.class);

    @Autowired
    private KbDocDao kbDocDao;

    @Value("${oa.emp-service.base-url}")
    private String empServiceBaseUrl;

    @Override
    public RESP selectPage(String keyword, int currentPage, int pageSize) {
        PageHelper.startPage(currentPage, pageSize);
        List<KbDoc> list = kbDocDao.selectPage(keyword);
        PageInfo<KbDoc> pageInfo = new PageInfo<>(list);
        return RESP.ok(list, pageInfo.getPageNum(), (int) pageInfo.getTotal());
    }

    @Override
    public RESP add(KbDoc kbDoc, String keyword, int currentPage, int pageSize) {
        kbDocDao.insert(kbDoc);
        return selectPage(keyword, currentPage, pageSize);
    }

    @Override
    public RESP update(Integer id, KbDoc kbDoc, String keyword, int currentPage, int pageSize) {
        kbDoc.setId(id);
        kbDocDao.update(kbDoc);
        return selectPage(keyword, currentPage, pageSize);
    }

    @Override
    public RESP delete(Integer id, String keyword, int currentPage, int pageSize) {
        kbDocDao.deleteById(id);
        return selectPage(keyword, currentPage, pageSize);
    }

    @Override
    public RESP reloadIndex() {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = empServiceBaseUrl + "/ai/reload-index";
            ResponseEntity<String> response = restTemplate.postForEntity(url, null, String.class);
            return RESP.ok(response.getBody());
        } catch (Exception e) {
            log.error("Failed to reload AI index", e);
            return RESP.error("Failed to reload index: " + e.getMessage());
        }
    }
}
