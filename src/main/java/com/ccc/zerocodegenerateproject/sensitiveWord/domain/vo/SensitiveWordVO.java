package com.ccc.zerocodegenerateproject.sensitiveWord.domain.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SensitiveWordVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String word;       // 敏感词内容
    private String category;   // 分类：政治、色情、暴力等
    private Integer level;     // 敏感等级：1-替换, 2-拦截
    private LocalDateTime createTime;
}
