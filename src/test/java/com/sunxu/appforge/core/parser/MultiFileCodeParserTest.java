package com.sunxu.appforge.core.parser;

import com.sunxu.appforge.ai.model.MultiFileCodeResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiFileCodeParserTest {

    private final MultiFileCodeParser parser = new MultiFileCodeParser();

    @Test
    @DisplayName("能同时提取 html / css / js 三段代码")
    void shouldExtractAllThreeParts() {
        String input = "```html\n<div id=\"app\"></div>\n```\n"
                + "```css\n#app { color: red; }\n```\n"
                + "```js\nconsole.log(1)\n```";
        MultiFileCodeResult result = parser.parseCode(input);
        assertEquals("<div id=\"app\"></div>", result.getHtmlCode());
        assertEquals("#app { color: red; }", result.getCssCode());
        assertEquals("console.log(1)", result.getJsCode());
    }

    @Test
    @DisplayName("```javascript 与 ```js 等价")
    void shouldAcceptJavascriptAlias() {
        String input = "```javascript\nvar a = 1;\n```";
        assertEquals("var a = 1;", parser.parseCode(input).getJsCode());
    }

    @Test
    @DisplayName("缺少 CSS 时字段保持 null，而不是空串")
    void shouldKeepNullWhenCssMissing() {
        String input = "```html\n<p>x</p>\n```\n```js\nalert(1)\n```";
        MultiFileCodeResult result = parser.parseCode(input);
        assertNull(result.getCssCode());
    }

    @Test
    @DisplayName("其它语言的代码块不会被误识别")
    void shouldIgnoreOtherLanguages() {
        String input = "```java\nSystem.out.println(1);\n```";
        MultiFileCodeResult result = parser.parseCode(input);
        assertNull(result.getHtmlCode());
        assertNull(result.getCssCode());
        assertNull(result.getJsCode());
    }

    @Test
    @DisplayName("提取结果会去掉首尾空白")
    void shouldTrimExtractedCode() {
        String input = "```css\n\n  body { margin: 0 }  \n\n```";
        String css = parser.parseCode(input).getCssCode();
        assertTrue(css.startsWith("body"));
        assertTrue(css.endsWith("}"));
    }
}
