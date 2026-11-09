package com.sunxu.appforge.ai.tools;

import com.sunxu.appforge.constant.AppConstant;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * 文件写入工具
 * 支持 AI 通过工具调用的方式写入文件
 */
@Slf4j
public class FileWriteTool {

    @Tool("写入文件到指定路径")
    public String writeFile(
            @P("文件的相对路径")
            String relativeFilePath,
            @P("要写入文件的内容")
            String content,
            @ToolMemoryId Long appId
    ) {
        try {
            // 参数兜底校验：残缺的工具调用返回明确错误信息（不能抛无消息异常，
            // 否则 langchain4j 的 DefaultToolExecutor 会返回 null 导致
            // ToolExecutionResultMessage 构造失败，整个生成流程崩溃）
            if (relativeFilePath == null || relativeFilePath.isBlank()) {
                return "文件写入失败: 文件路径不能为空";
            }
            if (content == null) {
                content = "";
            }
            if (appId == null) {
                return "文件写入失败: 应用 ID 为空";
            }
            Path path = Paths.get(relativeFilePath);
            if (!path.isAbsolute()) {
                // 相对路径处理，创建基于 appId 的项目目录
                String projectDirName = "vue_project_" + appId;
                Path projectRoot = Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR, projectDirName);
                path = projectRoot.resolve(relativeFilePath);
            }
            // 创建父目录（如果不存在）
            Path parentDir = path.getParent();
            if (parentDir != null) {
                Files.createDirectories(parentDir);
            }
            // 写入文件内容
            Files.write(path, content.getBytes(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
            log.info("成功写入文件: {}", path.toAbsolutePath());
            // 注意要返回相对路径，不能让 AI 把文件绝对路径返回给用户
            return "文件写入成功: " + relativeFilePath;
        } catch (Exception e) {
            // 捕获所有异常（含 NPE 等运行时异常），保证工具永远返回非空字符串
            String errorMessage = "文件写入失败: " + relativeFilePath + ", 错误: "
                    + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            log.error(errorMessage, e);
            return errorMessage;
        }
    }
}
