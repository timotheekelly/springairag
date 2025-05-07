package com.mongodb.springairag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

@Service
public class RagService {

    private final ChatClient chatClient;

    public RagService(ChatModel chatModel, QuestionAnswerAdvisor advisor) {
        this.chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(advisor)
                .build();
    }

    public String ask(String userQuestion) {
        return chatClient.prompt()
                .user(userQuestion)
                .call()
                .content();
    }
}
