package com.oa2.config;

import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;
import org.springframework.data.elasticsearch.config.AbstractElasticsearchConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Configuration
@EnableElasticsearchRepositories(basePackages = "com.oa2.repository")
public class ElasticsearchConfig extends AbstractElasticsearchConfiguration {

    @Value("${elasticsearch.host:localhost}")
    private String elasticsearchHost;

    @Value("${elasticsearch.port:9200}")
    private int elasticsearchPort;

    /**
     *   创建并配置RestHighLevelClient Bean，连接Elasticsearch服务。具体功能如下：
     *     配置的主机和端口构建 ClientConfiguration；
     *     通过 RestClients.create() 方法创建 REST 客户端；
     *     返回 RestHighLevelClient 实例供 Spring 管理。
     * @return
     */

    @Override
    @Bean
    public RestHighLevelClient elasticsearchClient() {
        System.out.println("启动ES连接");
        final ClientConfiguration clientConfiguration = ClientConfiguration.builder()
                .connectedTo(elasticsearchHost + ":" + elasticsearchPort)
                .build();

        return RestClients.create(clientConfiguration).rest();
    }
} 