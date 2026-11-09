package com.sunxu.appforge.core;

import com.sunxu.appforge.ai.model.HtmlCodeResult;
import com.sunxu.appforge.ai.model.MultiFileCodeResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CodeParserTest {

    /**
     * 正常情况：AI 返回里带 ```html 代码块
     */
    @Test
    void parseHtmlCode_withCodeBlock() {
        String codeContent = """
                好的，这是你要的单页网站：

                ```html
                <!DOCTYPE html>
                <html>
                <head>
                    <title>测试页面</title>
                </head>
                <body>
                    <h1>Hello World!</h1>
                </body>
                </html>
                ```

                希望对你有帮助！
                """;
        HtmlCodeResult result = CodeParser.parseHtmlCode(codeContent);
        assertNotNull(result);
        assertNotNull(result.getHtmlCode());
        // 提取出来的应该只包含 html 标签内容，不包含外面的说明文字
        assertTrue(result.getHtmlCode().contains("<h1>Hello World!</h1>"));
        assertFalse(result.getHtmlCode().contains("希望对你有帮助"));
    }

    /**
     * 边界情况：AI 没包代码块，直接吐了一段 HTML
     * 按 CodeParser 的逻辑，会把整个内容当 HTML 返回
     */
    @Test
    void parseHtmlCode_withoutCodeBlock() {
        String codeContent = """
                <!DOCTYPE html>
                <html>
                <head><title>裸 HTML</title></head>
                <body><p>没有代码块</p></body>
                </html>
                """;
        HtmlCodeResult result = CodeParser.parseHtmlCode(codeContent);
        assertNotNull(result.getHtmlCode());
        assertTrue(result.getHtmlCode().contains("裸 HTML"));
    }

    /**
     * 正常情况：HTML + CSS + JS 三个代码块都有
     */
    @Test
    void parseMultiFileCode_withAllBlocks() {
        String codeContent = """
                这是一个完整的留言板：

                ```html
                <!DOCTYPE html>
                <html>
                <head>
                    <link rel="stylesheet" href="style.css">
                </head>
                <body>
                    <h1>留言板</h1>
                    <script src="script.js"></script>
                </body>
                </html>
                ```

                ```css
                h1 {
                    color: blue;
                    text-align: center;
                }
                ```

                ```js
                console.log('页面加载完成');
                ```

                以上就是完整代码！
                """;
        MultiFileCodeResult result = CodeParser.parseMultiFileCode(codeContent);
        assertNotNull(result);
        // 三段代码都应该被正确提取
        assertNotNull(result.getHtmlCode());
        assertNotNull(result.getCssCode());
        assertNotNull(result.getJsCode());
        assertTrue(result.getHtmlCode().contains("<h1>留言板</h1>"));
        assertTrue(result.getCssCode().contains("color: blue;"));
        assertTrue(result.getJsCode().contains("console.log"));
    }

    /**
     * 边界情况：只有 HTML，没有 CSS 和 JS
     * cssCode / jsCode 应该是 null
     */
    @Test
    void parseMultiFileCode_withOnlyHtml() {
        String codeContent = """
                ```html
                <html><body><p>只有 HTML</p></body></html>
                ```
                """;
        MultiFileCodeResult result = CodeParser.parseMultiFileCode(codeContent);
        assertNotNull(result.getHtmlCode());
        assertNull(result.getCssCode());
        assertNull(result.getJsCode());
    }
}
