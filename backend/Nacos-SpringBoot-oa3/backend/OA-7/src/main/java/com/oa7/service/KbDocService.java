package com.oa7.service;

import com.oa7.pojo.KbDoc;
import com.oa7.util.RESP;

public interface KbDocService {

    RESP selectPage(String keyword, int currentPage, int pageSize);

    RESP add(KbDoc kbDoc, String keyword, int currentPage, int pageSize);

    RESP update(Integer id, KbDoc kbDoc, String keyword, int currentPage, int pageSize);

    RESP delete(Integer id, String keyword, int currentPage, int pageSize);

    RESP reloadIndex();
}
