package com.oa2.controller;

import com.oa2.util.DataMigrationUtil;
import com.oa2.util.RESP;
import com.oa2.repository.SignElasticsearchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.web.bind.annotation.*;
import com.oa2.pojo.Sign;

/**
 * 数据迁移控制器
 * 提供数据迁移相关的 API 接口
 */
@RestController
@RequestMapping("/migration")
@CrossOrigin
public class DataMigrationController {

    @Autowired
    private DataMigrationUtil dataMigrationUtil;

    @Autowired(required = false)
    private ElasticsearchRestTemplate elasticsearchTemplate;

    @Autowired(required = false)
    private SignElasticsearchRepository signRepository;

    /**
     * 重建 Elasticsearch 索引
     */
    @PostMapping("/rebuild-index")
    public RESP rebuildIndex() {
        try {
            if (elasticsearchTemplate == null) {
                return RESP.error("Elasticsearch 模板未配置");
            }

            IndexOperations indexOps = elasticsearchTemplate.indexOps(Sign.class);
            
            // 删除现有索引（如果存在）
            if (indexOps.exists()) {
                indexOps.delete();
                System.out.println("已删除现有索引");
            }
            
            // 创建新索引
            indexOps.create();
            indexOps.putMapping(indexOps.createMapping());
            System.out.println("已重建索引");
            
            return RESP.ok("索引重建成功");
        } catch (Exception e) {
            e.printStackTrace();
            return RESP.error("索引重建失败：" + e.getMessage());
        }
    }

    /**
     * 验证 Elasticsearch 迁移结果
     */
    @GetMapping("/validate")
    public RESP validateMigration() {
        try {
            dataMigrationUtil.validateMigration();
            return RESP.ok("验证完成，请查看控制台日志");
        } catch (Exception e) {
            return RESP.error("验证失败：" + e.getMessage());
        }
    }

    /**
     * 获取迁移状态信息
     */
    @GetMapping("/status")
    public RESP getMigrationStatus() {
        try {
            if (elasticsearchTemplate != null) {
                IndexOperations indexOps = elasticsearchTemplate.indexOps(Sign.class);
                boolean indexExists = indexOps.exists();
                
                String status = "当前使用 Elasticsearch 存储签到记录，索引状态：" + 
                               (indexExists ? "已创建" : "未创建");
                
                if (signRepository != null && indexExists) {
                    long count = signRepository.count();
                    status += "，记录数量：" + count;
                }
                
                return RESP.ok(status);
            } else {
                return RESP.ok("当前使用 MySQL 存储签到记录");
            }
        } catch (Exception e) {
            return RESP.error("获取状态失败：" + e.getMessage());
        }
    }
} 