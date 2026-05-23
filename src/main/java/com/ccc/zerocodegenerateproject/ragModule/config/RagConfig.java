package com.ccc.zerocodegenerateproject.ragModule.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Duration;
import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

@Configuration
public class RagConfig {

    @Value("${aliyun.dashscope.api-key}")
    private String dashscopeApiKey;
    // 千问 URL
    @Value("${aliyun.dashscope.base-url}")
    private String dashScopeBaseUrl;


    /**
     * 阿里千问 向量化模型
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        return OpenAiEmbeddingModel.builder()
                .apiKey(dashscopeApiKey)
                .baseUrl(dashScopeBaseUrl)
                .modelName("text-embedding-v1")
                .maxRetries(3)
                .timeout(Duration.ofSeconds(60))
                .build();
    }

    /**
     * 获取 contentRetriever---RAG检索器
     */
    @Bean
    public ContentRetriever myRetriever(EmbeddingStore<TextSegment> embeddingStore,
                                        EmbeddingModel embeddingModel) {

        Filter isPublicFilter = metadataKey("isPublic").isEqualTo(1);
        Filter successFilter = metadataKey("status").isEqualTo("SUCCESS");

        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(4)
                .filter(isPublicFilter)
                .filter(successFilter)
                .minScore(0.6)
                .build();
    }
}
