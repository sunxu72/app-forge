package com.sunxu.appforge.core;

import com.sunxu.appforge.model.entity.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

    @Disabled("集成测试：需要真实 API Key 与 MySQL，本地/CI 默认不执行")
    @SpringBootTest
    class AiCodeGeneratorFacadeTest {

        @Resource
        private AiCodeGeneratorFacade aiCodeGeneratorFacade;

        @Test
        void generateAndSaveCode() {
            File file = aiCodeGeneratorFacade.generateAndSaveCode("任务记录网站", CodeGenTypeEnum.MULTI_FILE, 1L);
            Assertions.assertNotNull(file);
        }

        @Test
        void generateAndSaveCodeStream() {
            Flux<String> codeStream = aiCodeGeneratorFacade.generateAndSaveCodeStream("任务记录网站", CodeGenTypeEnum.MULTI_FILE, 1L, 1L);
            // 阻塞等待所有数据收集完成
            List<String> result = codeStream.collectList().block();
            // 验证结果
            Assertions.assertNotNull(result);
            String completeContent = String.join("", result);
            Assertions.assertNotNull(completeContent);
        }


    }
