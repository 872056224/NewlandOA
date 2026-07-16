package com.oa2.vector;

import lombok.Data;

/**
 * 向量库文档：一条知识（标准问题 + 答案）及其语义向量
 */
@Data
public class VectorDocument {

    /** 唯一标识 */
    private String id;

    /** 标准问题 */
    private String question;

    /** 答案 */
    private String answer;

    /** 分类：sign / account / leave / org / system / chitchat / custom */
    private String category;

    /** 来源：builtin=内置知识库（每次启动重建），custom=运行期动态添加（落盘持久化） */
    private String source;

    /** L2 归一化后的语义向量 */
    private float[] vector;
}
