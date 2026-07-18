package com.oa7.repository;

import com.oa7.pojo.Sign;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SignElasticsearchRepository extends ElasticsearchRepository<Sign, String> {
    
    /**
     * 根据员工编号和日期查询打卡记录
     */
    @Query("{\"bool\":{\"must\":[{\"term\":{\"number\":\"?0\"}},{\"wildcard\":{\"signDate\":\"?1*\"}}]}}")
    List<Sign> findByNumberAndSignDateStartsWith(int number, String datePrefix);
    
    /**
     * 根据员工编号查询打卡记录（分页）
     */
    Page<Sign> findByNumberOrderByTimestampDesc(int number, Pageable pageable);
    
    /**
     * 查询所有记录（分页）
     */
    Page<Sign> findAllByOrderByTimestampDesc(Pageable pageable);
    
    /**
     * 统计员工的总打卡记录数
     */
    long countByNumber(int number);
    
    /**
     * 统计所有打卡记录数
     */
    long count();
    
    /**
     * 根据状态查询记录（分页）
     */
    Page<Sign> findByState(String state, Pageable pageable);
    
    /**
     * 统计指定状态的记录数
     */
    long countByState(String state);

    /**
     * 根据日期前缀查询记录（分页）
     */
    Page<Sign> findBySignDateStartsWith(String datePrefix, Pageable pageable);
    
    /**
     * 根据员工编号和日期前缀查询记录
     */
    List<Sign> findByNumberAndDateOnly(int number, String dateOnly);
    
    /**
     * 查询所有记录（不分页，用于统计）
     */
    List<Sign> findAllByOrderByTimestampDesc();
    
    /**
     * 根据日期查询所有记录
     */
    List<Sign> findByDateOnly(String dateOnly);

    /**
     * 统计指定日期前缀和状态的记录数
     */
    long countByDateOnlyAndState(String dateOnly, String state);

    /**
     * 根据日期前缀和状态查询记录（分页）
     */
    Page<Sign> findByDateOnlyAndState(String dateOnly, String state, Pageable pageable);
    

    
} 