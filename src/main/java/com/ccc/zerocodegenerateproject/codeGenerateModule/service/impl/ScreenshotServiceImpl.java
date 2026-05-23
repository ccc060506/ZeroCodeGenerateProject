package com.ccc.zerocodegenerateproject.codeGenerateModule.service.impl;

import com.ccc.zerocodegenerateproject.codeGenerateModule.domain.entity.GeneratePageResult;
import com.ccc.zerocodegenerateproject.codeGenerateModule.service.ScreenshotService;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.LoadState;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

@Service
public class ScreenshotServiceImpl implements ScreenshotService {

    @Override
    public String renderAndScreenshot(GeneratePageResult result) {
        String code = (String) result.getPageGeneratedCode();
        if (code == null || code.isBlank()) {
            throw new RuntimeException("页面代码为空，无法截图");
        }

        // 提取 <template> 内容
        String template = extractTemplate(code);

        // 构建完整 HTML（Vue3 + ElementPlus CDN）
        String html = buildFullHtml(template);

        Path tempFile;
        try {
            tempFile = Files.createTempFile("preview-", ".html");
            Files.writeString(tempFile, html, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("创建临时文件失败", e);
        }

        // Playwright 截图
        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true))) {

            Page page = browser.newPage();
            page.setViewportSize(1280, 800);
            page.navigate(tempFile.toUri().toString());
            page.waitForLoadState(LoadState.NETWORKIDLE);
            page.waitForTimeout(3000); // 等 CDN 加载 + Vue 挂载

            byte[] screenshotBytes = page.screenshot(new Page.ScreenshotOptions().setFullPage(true));
            return Base64.getEncoder().encodeToString(screenshotBytes);

        } catch (Exception e) {
            throw new RuntimeException("截图失败", e);
        } finally {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException ignored) {
            }
        }
    }

    /** 从 Vue SFC 中提取 <template> 内容 */
    private String extractTemplate(String code) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "<template[^>]*>([\\s\\S]*?)</template>", java.util.regex.Pattern.CASE_INSENSITIVE
        );
        java.util.regex.Matcher matcher = pattern.matcher(code);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        // 没有 template 标签，直接用全部内容
        return code.trim();
    }

    /** 构建可渲染的完整 HTML */
    private String buildFullHtml(String template) {
        String escapedTemplate = escapeForJs(template);
        return "<!DOCTYPE html>\n<html lang=\"zh-CN\">\n<head>\n"
            + "<meta charset=\"UTF-8\">\n"
            + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
            + "<link rel=\"stylesheet\" href=\"https://unpkg.com/element-plus/dist/index.css\">\n"
            + "<style>body{margin:0;padding:12px;background:#f5f7fa;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif}</style>\n"
            + "</head>\n<body>\n<div id=\"app\"></div>\n"
            + "<script src=\"https://unpkg.com/vue@3/dist/vue.global.prod.js\"></script>\n"
            + "<script src=\"https://unpkg.com/element-plus/dist/index.full.min.js\"></script>\n"
            + "<script>try{var tpl=" + escapedTemplate
            + ";var app=Vue.createApp({template:tpl});app.use(ElementPlus);app.mount('#app')}"
            + "catch(e){document.getElementById('app').innerHTML='<pre style=\"color:red\">Error: '+e.message+'</pre>'}"
            + "</script>\n</body>\n</html>";
    }

    /** 把模板字符串安全嵌入 JavaScript（防止 </script> 提前闭合） */
    private String escapeForJs(String s) {
        StringBuilder sb = new StringBuilder();
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c == '<' && i + 8 < s.length()
                        && s.substring(i, i + 9).equalsIgnoreCase("</script>")) {
                        sb.append("<\\/script>");
                        i += 8;
                    } else if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
        return sb.toString();
    }
}
