package com.oa2.pojo;

import lombok.Data;

/**
 * AI 客服知识库文档
 */
@Data
public class KbDoc {

    private Integer id;

    /** 标准问题 */
    private String question;

    /** 答案 */
    private String answer;

    /** 辅助召回关键词（空格分隔），参与向量化以提升召回率 */
    private String keywords;

    /** 是否热门问题（对话页首屏推荐） */
    private Boolean hot;

    /** 是否启用 */
    private Boolean enabled;

    private String createTime;
}
