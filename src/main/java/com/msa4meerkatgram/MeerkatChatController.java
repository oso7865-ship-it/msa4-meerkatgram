package com.msa4meerkatgram;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeerkatChatController {

    private final ChatClient chatClient;

    // 스프링이 알아서 제미나이와 연결된 통신망(ChatClient)을 만들어줍니다.
    public MeerkatChatController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    // 인터넷 주소창에 /test-gemini 라고 치면 이 부분이 실행됩니다!
    @GetMapping("/test-gemini")
    public String askGemini(@RequestParam(defaultValue = "안녕? 넌 누구야?") String message) {

        System.out.println("내가 보낸 질문: " + message);

        // 제미나이에게 질문을 던지고, 답변 텍스트만 쏙 뽑아옵니다.
        String response = chatClient.prompt()
                              .user(message)
                              .call()
                              .content();

        return "🤖 제미나이의 답변: " + response;
    }
}