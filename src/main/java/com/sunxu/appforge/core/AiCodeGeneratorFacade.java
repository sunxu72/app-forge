package com.sunxu.appforge.core;

import com.sunxu.appforge.ai.AiCodeGeneratorService;
import com.sunxu.appforge.ai.VueProjectGeneratorService;
import com.sunxu.appforge.ai.model.HtmlCodeResult;
import com.sunxu.appforge.ai.model.MultiFileCodeResult;
import com.sunxu.appforge.core.parser.CodeParserExecutor;
import com.sunxu.appforge.core.saver.CodeFileSaverExecutor;
import com.sunxu.appforge.exception.BusinessException;
import com.sunxu.appforge.exception.ErrorCode;
import com.sunxu.appforge.model.entity.ChatMessage;
import com.sunxu.appforge.model.entity.enums.CodeGenTypeEnum;
import com.sunxu.appforge.service.ChatMessageService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

/**
 * AI 代码生成外观类，组合生成和保存功能
 */
@Slf4j
@Service
public class AiCodeGeneratorFacade {

    /**
     * 流式生成完成标记（前端据此判定生成成功并刷新预览）
     */
    public static final String GENERATE_DONE_MARKER = "__GENERATE_DONE__";

    /**
     * 流式生成失败标记（AI 输出解析或文件保存失败时发送）
     */
    public static final String GENERATE_ERROR_MARKER = "__GENERATE_ERROR__";

    @Resource
    private AiCodeGeneratorService aiCodeGeneratorService;

    @Resource
    private VueProjectGeneratorService vueProjectGeneratorService;

    @Resource
    private ChatMessageService chatMessageService;

    /**
     * 统一入口：根据类型生成并保存代码（使用 appId）
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @return 保存的目录
     */
    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        return switch (codeGenTypeEnum) {
            case HTML -> {
                HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                MultiFileCodeResult result = aiCodeGeneratorService.generateMultiFileCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.MULTI_FILE, appId);
            }
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    /**
     * 统一入口：根据类型生成并保存代码（流式，使用 appId）
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @param appId           应用 ID
     * @param userId          用户 ID（用于记录对话历史）
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId, Long userId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        return switch (codeGenTypeEnum) {
            case HTML -> {
                Flux<String> codeStream = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
                yield processCodeStream(codeStream, CodeGenTypeEnum.HTML, appId, userId);
            }
            case MULTI_FILE -> {
                Flux<String> codeStream = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
                yield processCodeStream(codeStream, CodeGenTypeEnum.MULTI_FILE, appId, userId);
            }
            case VUE_PROJECT -> {
                // Vue 项目：AI 通过 FileWriteTool 工具调用直接把工程文件写入磁盘，
                // 返回的流是生成过程文字说明，无需再走代码解析/保存
                Flux<String> textStream = vueProjectGeneratorService.generateVueProjectCodeStream(appId, userMessage);
                StringBuilder textBuilder = new StringBuilder();
                // 记录生成是否成功，用于向前端发送成功/失败标记
                AtomicReference<Boolean> generateSuccess = new AtomicReference<>(true);
                yield textStream.doOnNext(textBuilder::append)
                        .doOnComplete(() -> {
                            // 生成完成，记录 AI 消息
                            saveAiMessage(appId, userId, textBuilder.toString(), "success");
                        })
                        .doOnError(e -> {
                            generateSuccess.set(false);
                            log.error("Vue 项目生成异常", e);
                            saveAiMessage(appId, userId, textBuilder.toString(), "error");
                        })
                        // 吞掉异常，保证 SSE 流不中断，由 concatWith 统一发送标记
                        .onErrorResume(e -> Flux.empty())
                        .concatWith(Mono.fromSupplier(() ->
                                generateSuccess.get() ? GENERATE_DONE_MARKER : GENERATE_ERROR_MARKER));
            }
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    /**
     * 通用流式代码处理方法（使用 appId）
     *
     * @param codeStream  代码流
     * @param codeGenType 代码生成类型
     * @param appId       应用 ID
     * @param userId      用户 ID（用于记录对话历史）
     * @return 流式响应
     */
    private Flux<String> processCodeStream(Flux<String> codeStream, CodeGenTypeEnum codeGenType, Long appId, Long userId) {
        StringBuilder codeBuilder = new StringBuilder();
        // 记录保存是否成功，用于向前端发送成功/失败标记
        AtomicReference<Boolean> saveSuccess = new AtomicReference<>(true);
        return codeStream.doOnNext(chunk -> {
            // 实时收集代码片段
            codeBuilder.append(chunk);
        }).doOnComplete(() -> {
            // 流式返回完成后保存代码，并记录 AI 消息
            try {
                String completeCode = codeBuilder.toString();
                // 使用执行器解析代码
                Object parsedResult = CodeParserExecutor.executeParser(completeCode, codeGenType);
                // 使用执行器保存代码
                File savedDir = CodeFileSaverExecutor.executeSaver(parsedResult, codeGenType, appId);
                log.info("保存成功，路径为：" + savedDir.getAbsolutePath());
                // 保存 AI 成功消息
                saveAiMessage(appId, userId, completeCode, "success");
            } catch (Exception e) {
                saveSuccess.set(false);
                log.error("保存失败: {}", e.getMessage());
                // AI 输出存在但解析/保存失败，记录失败消息
                saveAiMessage(appId, userId, codeBuilder.toString(), "error");
            }
        }).doOnError(e -> {
            // AI 流式生成异常中断（如模型调用失败），记录失败消息，保留已收集的部分内容
            log.error("AI 流式生成异常", e);
            saveAiMessage(appId, userId, codeBuilder.toString(), "error");
        }).concatWith(Mono.fromSupplier(() ->
                saveSuccess.get() ? GENERATE_DONE_MARKER : GENERATE_ERROR_MARKER));
    }

    /**
     * 保存 AI 消息到对话历史（失败不阻断生成流程，仅记录日志）
     *
     * @param appId   应用 ID
     * @param userId  用户 ID
     * @param content 消息内容
     * @param status  生成状态（success / error）
     */
    private void saveAiMessage(Long appId, Long userId, String content, String status) {
        try {
            ChatMessage aiMessage = ChatMessage.builder()
                    .appId(appId)
                    .userId(userId)
                    .messageType("ai")
                    .content(content)
                    .status(status)
                    .build();
            chatMessageService.save(aiMessage);
        } catch (Exception e) {
            log.error("保存 AI 消息失败: {}", e.getMessage());
        }
    }


}
