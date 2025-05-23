package com.example.mongo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.nio.charset.Charset;
import java.util.List;

@ImportRuntimeHints(MongoApplication.Hints.class)
@SpringBootApplication
public class MongoApplication {

    public static void main(String[] args) {
        SpringApplication.run(MongoApplication.class, args);
    }

    static final Resource DOGS_JSON_FILE = new ClassPathResource("/dogs.json");

    static class Hints implements RuntimeHintsRegistrar {

        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            hints.resources().registerResource(DOGS_JSON_FILE);
        }
    }

    @Bean
    ApplicationRunner mongoDbInitialzier(MongoTemplate template,
                                         VectorStore vectorStore,
                                         @Value("${spring.ai.vectorstore.mongodb.collection-name}") String collectionName,
                                         ObjectMapper objectMapper) {
        return args -> {
            if (template.collectionExists(collectionName) && template.estimatedCount(collectionName) > 0)
                return;

            var documentData = DOGS_JSON_FILE.getContentAsString(Charset.defaultCharset());
            var jsonNode = objectMapper.readTree(documentData);
            jsonNode.spliterator().forEachRemaining(jsonNode1 -> {
                var id = jsonNode1.get("id").intValue();
                var name = jsonNode1.get("name").textValue();
                var description = jsonNode1.get("description").textValue();
                var dogument = new Document("id: %s, name: %s, description: %s".formatted(
                        id, name, description
                ));
                vectorStore.add(List.of(dogument));
            });
        };
    }

}


@Controller
@ResponseBody
class AdoptionController {

    private final ChatClient ai;
    private final InMemoryChatMemoryRepository memoryRepository;

    AdoptionController(ChatClient.Builder ai, VectorStore vectorStore) {
        var system = """
                You are an AI-powered assistant to help people adopt a dog from the adoption
                agency named Pooch Palace with locations in Antwerp, Seoul, Tokyo, Singapore, Paris,
                Mumbai, New Delhi, Barcelona, San Francisco, and London. Information about the dogs available
                will be presented below. If there is no information, then return a polite response suggesting we
                don't have any dogs available.
                """;

        this.memoryRepository = new InMemoryChatMemoryRepository();

        this.ai = ai
                .defaultSystem(system)
                .defaultAdvisors(new QuestionAnswerAdvisor(vectorStore))
                .build();
    }

    @GetMapping("/{user}/dogs/assistant")
    String inquire(@PathVariable String user, @RequestParam String question) {

        var memory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(memoryRepository)
                .maxMessages(10)
                .build();

        var memoryAdvisor = MessageChatMemoryAdvisor.builder(memory).build();

        return this.ai
                .prompt()
                .user(question)
                .advisors(a -> a
                        .advisors(memoryAdvisor)
                        .param(ChatMemory.CONVERSATION_ID, user)
                )
                .call()
                .content();
    }

}