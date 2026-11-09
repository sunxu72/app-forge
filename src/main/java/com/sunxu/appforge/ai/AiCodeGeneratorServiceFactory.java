package com.sunxu.appforge.ai;

import com.sunxu.appforge.ai.tools.FileWriteTool;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiCodeGeneratorServiceFactory {

    @Resource
    private ChatModel chatModel;


    /**
     * 注意：项目里存在 2 个 StreamingChatModel Bean
     * （自定义 reasoningStreamingChatModel + starter 自动配置的 openAiStreamingChatModel），
     * 这里按名称指定注入自定义的推理流式模型（用于 Vue 项目生成/工具调用）
     */
    @Resource(name = "reasoningStreamingChatModel")
    private StreamingChatModel streamingChatModel;

    
    @Bean
    public AiCodeGeneratorService aiCodeGeneratorService() {
        return AiServices.builder(AiCodeGeneratorService.class)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                .build();
    }

    /**
     * Vue 项目生成服务：注入 FileWriteTool 工具 + 按 appId 分区维护对话记忆
     * 与基础生成服务拆分，避免 HTML/多文件模式被工具调用干扰
     */
    @Bean
    public VueProjectGeneratorService vueProjectGeneratorService() {
        return AiServices.builder(VueProjectGeneratorService.class)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                // 注入文件写入工具，AI 可通过工具调用把 Vue 工程文件逐个写入磁盘
                .tools(new FileWriteTool())
                // 按 appId 分区维护多轮对话记忆（最近 10 条），模型可记住上文
                .chatMemoryProvider(chatMemoryId ->
                        MessageWindowChatMemory.builder()
                                .maxMessages(10)
                                .build())
                .build();
    }
}

