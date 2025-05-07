package com.mongodb.springairag;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest()
public class RagServiceDataTest {

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private RagService ragService;

    @BeforeEach
    void seedTestData() {
        List<Document> documents = List.of(
                new Document("1", "Spring AI is a Java framework for integrating AI into Spring applications.", Map.of("topic", "Spring")),
                new Document("2", "MongoDB Atlas Vector Search allows you to perform semantic search over your documents.", Map.of("topic", "MongoDB")),
                new Document("3", "Retrieval-Augmented Generation (RAG) improves LLM responses by providing external context.", Map.of("topic", "RAG"))
        );

        vectorStore.add(documents);

    }


    @Test
    void testRagReturnsContextualAnswer() {
        String question = "What is Spring AI?";
        String answer = ragService.ask(question);

        System.out.println("RAG answer: " + answer);
        assertThat(answer).isNotBlank();
        assertThat(answer).containsIgnoringCase("spring");
    }
}
