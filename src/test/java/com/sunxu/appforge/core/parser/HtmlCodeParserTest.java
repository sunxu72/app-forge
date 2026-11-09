package com.sunxu.appforge.core.parser;

import com.sunxu.appforge.ai.model.HtmlCodeResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class HtmlCodeParserTest {

    private final HtmlCodeParser parser = new HtmlCodeParser();

    @Test
    @DisplayName("能从 ```html 代码块中提取代码，忽略块外文字")
    void shouldExtractFencedHtml() {
        String input = "好的，代码如下：\n```html\n<html><body>hi</body></html>\n```\n以上。";
        HtmlCodeResult result = parser.parseCode(input);
        assertEquals("<html><body>hi</body></html>", result.getHtmlCode());
    }

    @Test
    @DisplayName("代码块语言标记大小写不敏感")
    void shouldMatchFenceCaseInsensitively() {
        String input = "```HTML\n<div>ok</div>\n```";
        assertEquals("<div>ok</div>", parser.parseCode(input).getHtmlCode());
    }

    @Test
    @DisplayName("没有代码块时，整段内容作为 HTML")
    void shouldFallbackToWholeContent() {
        String input = "<p>纯 HTML</p>";
        assertEquals("<p>纯 HTML</p>", parser.parseCode(input).getHtmlCode());
    }

    @Test
    @DisplayName("结果中不应残留代码块标记")
    void shouldNotContainFenceInResult() {
        String html = parser.parseCode("```html\n<span>x</span>\n```").getHtmlCode();
        assertEquals(false, html.contains("```"));
    }

    @Test
    @DisplayName("空白输入不抛异常")
    void shouldHandleBlankInput() {
        HtmlCodeResult result = parser.parseCode("   ");
        assertEquals("", result.getHtmlCode());
        assertNull(result.getDescription());
    }
}
