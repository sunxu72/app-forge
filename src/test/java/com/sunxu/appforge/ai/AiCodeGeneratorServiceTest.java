package com.sunxu.appforge.ai;

import com.sunxu.appforge.ai.model.HtmlCodeResult;
import com.sunxu.appforge.ai.model.MultiFileCodeResult;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
    class AiCodeGeneratorServiceTest {

        @Resource
        private AiCodeGeneratorService aiCodeGeneratorService;

        @Test
        void generateHtmlCode() {
            HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode("做个程序员鱼皮的工作记录小工具");
            Assertions.assertNotNull(result);
        }

        @Test
        void generateMultiFileCode() {
            MultiFileCodeResult result = aiCodeGeneratorService.generateMultiFileCode("做个程序员鱼皮的留言板,不超过20行");
            Assertions.assertNotNull(result);
        }
    }


