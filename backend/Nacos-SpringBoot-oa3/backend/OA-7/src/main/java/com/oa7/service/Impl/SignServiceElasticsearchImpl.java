package com.oa7.service.Impl;

import com.oa7.dao.EmpDao;
import com.oa7.pojo.Emp;
import com.oa7.pojo.O;
import com.oa7.pojo.Sign;
import com.oa7.repository.SignElasticsearchRepository;
import com.oa7.service.SignService;
import com.oa7.util.DU;
import com.oa7.util.LocationUtil;
import com.oa7.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.UUID;


//@Service
public class SignServiceElasticsearchImpl {

}