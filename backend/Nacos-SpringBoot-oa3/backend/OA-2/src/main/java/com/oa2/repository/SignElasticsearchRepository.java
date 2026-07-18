package com.oa2.repository;

import com.oa2.pojo.Sign;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SignElasticsearchRepository extends ElasticsearchRepository<Sign, String> {
    
    /**
     * 根据员工编号 日期 查询当天打卡记录
     */
    @Query("{\"bool\":{\"must\":[{\"term\":{\"number\":\"?0\"}},{\"wildcard\":{\"signDate\":\"?1*\"}}]}}")
    List<Sign> findByNumberAndSignDateStartsWith(int number, String datePrefix);

    /**
     * 根据员工编号和日期前缀查询今日签到记录
     */
    @Query("{\"bool\":{\"must\":[{\"term\":{\"number\":\"?0\"}},{\"wildcard\":{\"dateOnly\":\"?1*\"}}]}}")
    List<Sign> findByNumberAndDateOnly(int number, String dateOnly);


    /**
     * 根据员工编号查询打卡记录（分页）
     */
    Page<Sign> findByNumberOrderByTimestampDesc(int number, Pageable pageable);
    
    /**
     * 统计员工的总打卡记录数
     */
    long countByNumber(int number);

    /**
     * 查询所有记录（用于统计）
     */
    List<Sign> findAllByOrderByTimestampDesc();



    /**
     * 根据员工编号、签到日期和类型查询（精确匹配日期前缀）
     */
    @Query("{\"bool\":{\"must\":[{\"term\":{\"number\":\"?0\"}},{\"wildcard\":{\"signDate\":\"?1*\"}},{\"term\":{\"type\":\"?2\"}}]}}")
    Sign findByNumberAndSignDatePrefixAndType(int number, String datePrefix, String type);
    
    /**
     * 根据员工编号、签到日期和类型查询（完全匹配）
     */
    @Query("{\"bool\":{\"must\":[{\"term\":{\"number\":\"?0\"}},{\"term\":{\"signDate\":\"?1\"}},{\"term\":{\"type\":\"?2\"}}]}}")
    Sign findByNumberAndSignDateAndType(int number, String signDate, String type);
    
    /**
     * 根据日期和状态统计
     */
    @Query("{\"bool\":{\"must\":[{\"wildcard\":{\"signDate\":\"?0*\"}},{\"term\":{\"state\":\"?1\"}},{\"term\":{\"type\":\"a\"}}]}}")
    List<Sign> findByDatePrefixAndStateAndType(String datePrefix, String state, String type);

    /**
     * 根据日期范围查询
     */
    @Query("{\"range\":{\"dateOnly\":{\"gte\":\"?0\",\"lte\":\"?1\"}}}")
    List<Sign> findByDateRange(String startDate, String endDate);
    


} 