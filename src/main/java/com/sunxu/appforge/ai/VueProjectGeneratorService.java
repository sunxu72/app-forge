package com.sunxu.appforge.ai;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import reactor.core.publisher.Flux;

/**
 * Vue 项目生成服务（独立接口）
 * <p>
 * 与 HTML / 多文件生成拆分开：只有该接口注入了 FileWriteTool 工具，
 * 避免多文件模式被工具调用干扰。
 */
public interface VueProjectGeneratorService {

    /**
     * 生成 Vue 项目代码（流式）
     *
     * @param appId       应用 ID（作为记忆分区，同时用于 vue_project_{appId} 目录命名）
     * @param userMessage 用户消息
     * @return 生成过程的流式响应
     */
    @SystemMessage(fromResource = "prompt/codegen-vue-project-system-prompt.txt")
    Flux<String> generateVueProjectCodeStream(@MemoryId long appId, @UserMessage String userMessage);

}
