package com.ccc.zerocodegenerateproject.sensitiveWord.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SensitiveWordDTO {
    private String word;       // 敏感词内容
    private String category;   // 分类：政治、色情、暴力等
    private Integer level;     // 敏感等级：1-替换, 2-拦截
}
