package com.ccc.zerocodegenerateproject.sensitiveWord.controller;

import com.ccc.zerocodegenerateproject.common.result.Result;
import com.ccc.zerocodegenerateproject.sensitiveWord.service.ISensitiveWordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/sensitiveWord")
public class SensitiveWordController {

    @Autowired
    private ISensitiveWordService sensitiveWordService;

    // 构建和保存到redis
    @PostMapping("/refresh")
    public Result<String> refreshDfa() {
        try {
            List<String> words = sensitiveWordService.getAllValidWords();
            if (words.isEmpty()) {
                return Result.success("无有效敏感词，无需刷新");
            }
            sensitiveWordService.buildAndSaveToRedis(words);
            return Result.success("敏感词 DFA 树已刷新，词數：" + words.size());
        } catch (Exception e) {
            log.error("刷新敏感词 DFA 失败", e);
            return Result.error("刷新失败：" + e.getMessage());
        }
    }
}
