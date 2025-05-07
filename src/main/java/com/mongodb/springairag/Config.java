package com.mongodb.springairag;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaModel;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.ollama.management.ModelManagementOptions;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.mongodb.atlas.MongoDBAtlasVectorStore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;



@Configuration
@EnableAutoConfiguration
public class Config {

    @Bean
    public EmbeddingModel embeddingModel() {
        OllamaOptions options = OllamaOptions.builder()
                .model("nomic-embed-text")
                .build();

        return OllamaEmbeddingModel.builder()
                .ollamaApi(OllamaApi.builder().build())
                .defaultOptions(options)
                .observationRegistry(ObservationRegistry.NOOP)
                .modelManagementOptions(ModelManagementOptions.defaults())
                .build();
    }


    @Bean
    public ChatModel chatModel() {
        OllamaOptions options = OllamaOptions.builder()
                .model(OllamaModel.MISTRAL)
                .temperature(0.9)
                .build();

        return OllamaChatModel.builder()
                .ollamaApi(OllamaApi.builder().build())
                .defaultOptions(options)
                .toolCallingManager(ToolCallingManager.builder().build())
                .observationRegistry(ObservationRegistry.NOOP)
                .modelManagementOptions(ModelManagementOptions.defaults())
                .build();
    }

    @Bean
    public VectorStore mongodbVectorStore(MongoTemplate mongoTemplate, EmbeddingModel embeddingModel) {
        return MongoDBAtlasVectorStore.builder(mongoTemplate, embeddingModel)
                .collectionName("vector_store")
                .vectorIndexName("vector_index")
                .pathName("embedding")
                .initializeSchema(true)
                .build();
    }

    @Bean
    public QuestionAnswerAdvisor questionAnswerAdvisor(VectorStore vectorStore) {
        return QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(SearchRequest.builder()
                        .similarityThreshold(0.8d)
                        .topK(6)
                        .build())
                .build();
    }

}