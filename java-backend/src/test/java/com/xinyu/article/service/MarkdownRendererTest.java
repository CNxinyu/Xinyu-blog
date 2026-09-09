package com.xinyu.article.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownRendererTest {
    private final MarkdownRenderer renderer = new MarkdownRenderer();

    @Test
    void rendersGfmTableHeadingAndCodeLanguage() {
        String html = renderer.render("# Guide\n\n| Name | Value |\n| --- | --- |\n| Java | 21 |\n\n```java\nSystem.out.println(1);\n```");

        assertThat(html).contains("<h1 id=\"guide\">Guide</h1>")
                .contains("<table>")
                .contains("class=\"language-java\"");
    }

    @Test
    void escapesRawHtml() {
        assertThat(renderer.render("<script>alert('xss')</script>"))
                .contains("&lt;script&gt;")
                .doesNotContain("<script>");
    }
}
